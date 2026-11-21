import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require('/usr/local/lib/node_modules/@firebase/rules-unit-testing');
const {
  doc,
  setDoc,
  getDoc,
  updateDoc,
  deleteDoc,
  collection,
  query,
  where,
  getDocs,
  serverTimestamp,
  setLogLevel,
} = require('/usr/local/lib/node_modules/@firebase/rules-unit-testing/node_modules/firebase/firestore');

setLogLevel('error');

const PROJECT_ID = 'demo-txlsocial-rules';
const OWNER_UID = 'owner_uid_1';
const OWNER_EMAIL = 'abukeje26@gmail.com';
const ALICE_UID = 'alice_uid_1';
const BOB_UID = 'bob_uid_1';

let testEnv;

test.before(async () => {
  const rules = fs.readFileSync('firestore.rules', 'utf8');
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: '127.0.0.1',
      port: 8085,
    },
  });
});

test.after(async () => {
  await testEnv?.cleanup();
});

test.beforeEach(async () => {
  await testEnv.clearFirestore();
});

function privateUserPayload(uid, email, emailVerified = true, rank = 'none') {
  return {
    userId: uid,
    username: `User_${uid.slice(0, 6)}`,
    email,
    emailVerified,
    bio: 'Hola NexoLive',
    customStatus: 'En linea',
    avatarFrameId: 'neon_pulse',
    presetAvatarKey: 'anime',
    communitiesCount: 0,
    followersCount: 0,
    postsCount: 0,
    coinsCount: 5000,
    giftsReceivedCount: 0,
    donorExp: 0,
    donorLevel: 1,
    totalCoinsDonated: 0,
    dailyStreakDay: 0,
    lastDailyCheckInAtMillis: 0,
    followHostsProgress: 0,
    watchLiveMinutesProgress: 0,
    gameCoinsEarnedProgress: 0,
    giftCoinsSentProgress: 0,
    claimedTaskIds: '',
    inventoryJson: '{}',
    mePlusMember: false,
    lastMePlusDrawAtMillis: 0,
    appRankId: rank,
    banned: false,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function publicUserPayload(uid, rank = 'none') {
  return {
    userId: uid,
    username: `User_${uid.slice(0, 6)}`,
    discoverable: true,
    bio: 'Hola NexoLive',
    customStatus: 'En linea',
    avatarFrameId: 'neon_pulse',
    presetAvatarKey: 'anime',
    communitiesCount: 0,
    followersCount: 0,
    postsCount: 0,
    giftsReceivedCount: 0,
    donorExp: 0,
    donorLevel: 1,
    appRankId: rank,
    banned: false,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

test('unauthenticated users cannot read or write any collection', async () => {
  const anon = testEnv.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(anon, 'users_public', ALICE_UID)));
  await assertFails(getDocs(query(collection(anon, 'communities'), where('visibility', '==', 'public'))));
  await assertFails(setDoc(doc(anon, 'posts', 'post_1'), { content: 'hack' }));
});

test('multi-user discovery & verified-owner-only rank/ban enforcement', async () => {
  const ownerCtx = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  });
  const unverifiedOwnerCtx = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: false,
  });
  const aliceCtx = testEnv.authenticatedContext(ALICE_UID, {
    email: 'alice@example.com',
    email_verified: true,
  });
  const bobCtx = testEnv.authenticatedContext(BOB_UID, {
    email: 'bob@example.com',
    email_verified: true,
  });

  // Unverified owner email cannot claim owner rank
  await assertFails(
    setDoc(
      doc(unverifiedOwnerCtx.firestore(), 'users_public', OWNER_UID),
      publicUserPayload(OWNER_UID, 'owner')
    )
  );

  // Verified owner, alice, and bob create their profiles
  await assertSucceeds(
    setDoc(
      doc(ownerCtx.firestore(), 'users', OWNER_UID),
      privateUserPayload(OWNER_UID, OWNER_EMAIL, true, 'owner')
    )
  );
  await assertSucceeds(
    setDoc(
      doc(ownerCtx.firestore(), 'users_public', OWNER_UID),
      publicUserPayload(OWNER_UID, 'owner')
    )
  );
  await assertSucceeds(
    setDoc(
      doc(aliceCtx.firestore(), 'users', ALICE_UID),
      privateUserPayload(ALICE_UID, 'alice@example.com', true, 'none')
    )
  );
  await assertSucceeds(
    setDoc(
      doc(aliceCtx.firestore(), 'users_public', ALICE_UID),
      publicUserPayload(ALICE_UID, 'none')
    )
  );
  await assertSucceeds(
    setDoc(
      doc(bobCtx.firestore(), 'users_public', BOB_UID),
      publicUserPayload(BOB_UID, 'none')
    )
  );

  // Alice & Bob find each other via discoverable query without seeing private emails
  const discovered = await assertSucceeds(
    getDocs(query(collection(bobCtx.firestore(), 'users_public'), where('discoverable', '==', true)))
  );
  assert.equal(discovered.size, 3);
  await assertFails(getDoc(doc(bobCtx.firestore(), 'users', ALICE_UID)));

  // Alice cannot self-promote or grant ranks or ban Bob
  await assertFails(
    updateDoc(doc(aliceCtx.firestore(), 'users_public', ALICE_UID), {
      appRankId: 'influencer',
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    updateDoc(doc(aliceCtx.firestore(), 'users_public', BOB_UID), {
      appRankId: 'moderator',
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    updateDoc(doc(aliceCtx.firestore(), 'users_public', BOB_UID), {
      banned: true,
      updatedAt: serverTimestamp(),
    })
  );

  // Verified Owner CAN grant verified rank & ban Bob
  await assertSucceeds(
    updateDoc(doc(ownerCtx.firestore(), 'users_public', ALICE_UID), {
      appRankId: 'influencer',
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    updateDoc(doc(ownerCtx.firestore(), 'users_public', BOB_UID), {
      banned: true,
      banReason: 'Spam',
      updatedAt: serverTimestamp(),
    })
  );
});

test('communities, multi-speaker voice_frames, gifts, audio messages, and donor level progression', async () => {
  const aliceCtx = testEnv.authenticatedContext(ALICE_UID, {
    email: 'alice@example.com',
    email_verified: true,
  });
  const bobCtx = testEnv.authenticatedContext(BOB_UID, {
    email: 'bob@example.com',
    email_verified: true,
  });

  await assertSucceeds(
    setDoc(doc(aliceCtx.firestore(), 'users', ALICE_UID), privateUserPayload(ALICE_UID, 'alice@example.com'))
  );
  await assertSucceeds(
    setDoc(doc(aliceCtx.firestore(), 'users_public', ALICE_UID), publicUserPayload(ALICE_UID))
  );
  await assertSucceeds(
    setDoc(doc(bobCtx.firestore(), 'users_public', BOB_UID), publicUserPayload(BOB_UID))
  );

  // Alice creates a room & joins voice
  await assertSucceeds(
    setDoc(doc(aliceCtx.firestore(), 'communities', 'comm_1'), {
      creatorId: ALICE_UID,
      name: 'Sala Global IRL',
      category: 'IRL Urbano',
      description: 'Charla mundial',
      coverKey: 'irl',
      badgeLabel: 'nueva',
      visibility: 'public',
      memberCount: 1,
      activeVoiceCount: 1,
      voiceLocked: false,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    setDoc(doc(aliceCtx.firestore(), 'communities/comm_1/voice_participants', ALICE_UID), {
      communityId: 'comm_1',
      userId: ALICE_UID,
      username: 'Alice',
      presetAvatarKey: 'anime',
      avatarFrameId: 'neon_pulse',
      appRankId: 'none',
      role: 'ADMIN',
      roomVisibility: 'public',
      muted: false,
      speaking: true,
      handRaised: false,
      joinedAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Alice & Bob stream simultaneous voice frames (each owns their own ring slots)
  await assertSucceeds(
    setDoc(doc(aliceCtx.firestore(), 'communities/comm_1/voice_frames', `${ALICE_UID}_0`), {
      communityId: 'comm_1',
      senderId: ALICE_UID,
      roomVisibility: 'public',
      seq: 1,
      sampleRate: 8000,
      codec: 'ULAW',
      payloadB64: 'AAAA////',
      rms: 0.45,
      createdAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    setDoc(doc(bobCtx.firestore(), 'communities/comm_1/voice_frames', `${BOB_UID}_0`), {
      communityId: 'comm_1',
      senderId: BOB_UID,
      roomVisibility: 'public',
      seq: 1,
      sampleRate: 8000,
      codec: 'ULAW',
      payloadB64: 'BBBB////',
      rms: 0.35,
      createdAt: serverTimestamp(),
    })
  );

  // Bob CANNOT overwrite Alice's voice frame
  await assertFails(
    setDoc(doc(bobCtx.firestore(), 'communities/comm_1/voice_frames', `${ALICE_UID}_0`), {
      communityId: 'comm_1',
      senderId: ALICE_UID,
      roomVisibility: 'public',
      seq: 99,
      sampleRate: 8000,
      codec: 'ULAW',
      payloadB64: 'HACK',
      rms: 0.9,
      createdAt: serverTimestamp(),
    })
  );

  // Alice sends a Money Gun gift to Bob, updates her Donor EXP/Level/Tasks, and increments Bob's giftsReceivedCount
  await assertSucceeds(
    setDoc(doc(aliceCtx.firestore(), 'communities/comm_1/gifts', 'gift_1'), {
      communityId: 'comm_1',
      channelType: 'VOICE',
      roomVisibility: 'public',
      senderId: ALICE_UID,
      senderName: 'Alice',
      senderAvatarKey: 'anime',
      senderFrameId: 'neon_pulse',
      recipientId: BOB_UID,
      recipientName: 'Bob',
      giftId: 'money_gun',
      giftName: 'Money Gun',
      giftEmoji: '🔫',
      coinCost: 500,
      comboCount: 1,
      createdAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    updateDoc(doc(aliceCtx.firestore(), 'users', ALICE_UID), {
      coinsCount: 4500,
      donorExp: 500,
      donorLevel: 3,
      totalCoinsDonated: 500,
      giftCoinsSentProgress: 500,
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    updateDoc(doc(aliceCtx.firestore(), 'users_public', ALICE_UID), {
      donorExp: 500,
      donorLevel: 3,
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    updateDoc(doc(aliceCtx.firestore(), 'users_public', BOB_UID), {
      giftsReceivedCount: 1,
      updatedAt: serverTimestamp(),
    })
  );

  // Voice note audioDataB64 chat message succeeds and queries cleanly
  await assertSucceeds(
    setDoc(doc(bobCtx.firestore(), 'communities/comm_1/messages', 'msg_audio_1'), {
      communityId: 'comm_1',
      channelType: 'VOICE',
      roomVisibility: 'public',
      senderId: BOB_UID,
      senderName: 'Bob',
      senderAvatarKey: 'gaming',
      senderFrameId: 'cyber_prism',
      senderAppRankId: 'none',
      senderRole: 'MEMBER',
      content: 'Nota de voz',
      reactionCount: 0,
      audioDataB64: 'AAAA',
      audioDurationMs: 2500,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  const voiceMsgs = await assertSucceeds(
    getDocs(
      query(
        collection(aliceCtx.firestore(), 'communities/comm_1/messages'),
        where('roomVisibility', '==', 'public'),
        where('channelType', '==', 'VOICE')
      )
    )
  );
  assert.equal(voiceMsgs.size, 1);
});

test('daily tasks, drifting bottles, posts, custom frames, and notifications enforce ownership', async () => {
  const aliceCtx = testEnv.authenticatedContext(ALICE_UID, {
    email: 'alice@example.com',
    email_verified: true,
  });
  const bobCtx = testEnv.authenticatedContext(BOB_UID, {
    email: 'bob@example.com',
    email_verified: true,
  });

  await assertSucceeds(
    setDoc(doc(aliceCtx.firestore(), 'users', ALICE_UID), privateUserPayload(ALICE_UID, 'alice@example.com'))
  );
  // Daily check-in + bag update succeeds for owner
  await assertSucceeds(
    updateDoc(doc(aliceCtx.firestore(), 'users', ALICE_UID), {
      dailyStreakDay: 2,
      donorExp: 20,
      inventoryJson: '{"ornament_xmas":1,"popsicle":1}',
      claimedTaskIds: 'task_follow_host',
      updatedAt: serverTimestamp(),
    })
  );

  // Bob notifies Alice, Alice marks read
  await assertSucceeds(
    setDoc(doc(bobCtx.firestore(), `users/${ALICE_UID}/notifications`, 'notif_1'), {
      userId: ALICE_UID,
      actorId: BOB_UID,
      actorName: 'Bob',
      actorAvatarKey: 'gaming',
      actorFrameId: 'cyber_prism',
      title: 'Nuevo seguidor',
      subtitle: 'Bob te empezó a seguir',
      category: 'FOLLOW',
      read: false,
      createdAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    updateDoc(doc(aliceCtx.firestore(), `users/${ALICE_UID}/notifications`, 'notif_1'), {
      read: true,
    })
  );

  // Public post & bottle creation
  await assertSucceeds(
    setDoc(doc(aliceCtx.firestore(), 'posts', 'post_1'), {
      authorId: ALICE_UID,
      authorName: 'Alice',
      authorAvatarKey: 'anime',
      authorFrameId: 'neon_pulse',
      authorAppRankId: 'none',
      visibility: 'public',
      content: 'Hola mundo!',
      likesCount: 0,
      commentsCount: 0,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    setDoc(doc(bobCtx.firestore(), 'drifting_bottles', 'bottle_1'), {
      authorId: BOB_UID,
      anonymousAlias: 'Eco Lunar #102',
      moodTag: 'Feliz',
      content: 'Saludos desde otro continente!',
      visibility: 'public',
      expiresAtMillis: Date.now() + 86400000,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test('coin economy: owner gifting, positive coin payouts, subtraction block, and paid custom frames', async () => {
  const ownerCtx = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  });
  const aliceCtx = testEnv.authenticatedContext(ALICE_UID, {
    email: 'alice@example.com',
    email_verified: true,
  });
  const bobCtx = testEnv.authenticatedContext(BOB_UID, {
    email: 'bob@example.com',
    email_verified: true,
  });

  await assertSucceeds(
    setDoc(doc(aliceCtx.firestore(), 'users', ALICE_UID), privateUserPayload(ALICE_UID, 'alice@example.com'))
  );
  await assertSucceeds(
    setDoc(doc(bobCtx.firestore(), 'users', BOB_UID), privateUserPayload(BOB_UID, 'bob@example.com'))
  );

  // Verified Owner can gift coins directly to Alice
  await assertSucceeds(
    updateDoc(doc(ownerCtx.firestore(), 'users', ALICE_UID), {
      coinsCount: 7000,
      updatedAt: serverTimestamp(),
    })
  );

  // Bob can credit Alice positively (e.g. gift/frame sale payout)
  await assertSucceeds(
    updateDoc(doc(bobCtx.firestore(), 'users', ALICE_UID), {
      coinsCount: 7250,
      updatedAt: serverTimestamp(),
    })
  );

  // Bob CANNOT subtract coins from Alice or modify unrelated private fields
  await assertFails(
    updateDoc(doc(bobCtx.firestore(), 'users', ALICE_UID), {
      coinsCount: 100,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    updateDoc(doc(bobCtx.firestore(), 'users', ALICE_UID), {
      coinsCount: 7500,
      banned: true,
      updatedAt: serverTimestamp(),
    })
  );

  // Alice can publish a paid custom frame with Store metadata
  await assertSucceeds(
    setDoc(doc(aliceCtx.firestore(), 'custom_frames', 'frame_paid_1'), {
      creatorId: ALICE_UID,
      creatorName: 'Alice',
      title: 'Marco Dragon',
      frameType: 'PNG',
      pngPath: 'https://example.com/dragon.png',
      primaryColorHex: 0xFFFFD166,
      secondaryColorHex: 0xFFFF4EC2,
      stylePatternId: 'ring_glow',
      thicknessDp: 5,
      sparkles: true,
      priceCoins: 250,
      description: 'Estilo dragon dorado neón',
      category: 'Marco de perfil',
      isFeatured: true,
      salesCount: 0,
      favoriteCount: 0,
      visibility: 'public',
      createdAt: serverTimestamp(),
    })
  );

  // Bob buys Alice's frame: increments salesCount and credits Alice's coins & creator stats
  await assertSucceeds(
    updateDoc(doc(bobCtx.firestore(), 'custom_frames', 'frame_paid_1'), {
      salesCount: 1,
    })
  );
  await assertSucceeds(
    updateDoc(doc(bobCtx.firestore(), 'users', ALICE_UID), {
      coinsCount: 7450,
      creatorSalesCount: 1,
      creatorEarningsCoins: 200,
      updatedAt: serverTimestamp(),
    })
  );
  // Bob toggles favoriteCount +1 and -1
  await assertSucceeds(
    updateDoc(doc(bobCtx.firestore(), 'custom_frames', 'frame_paid_1'), {
      favoriteCount: 1,
    })
  );
  await assertSucceeds(
    updateDoc(doc(bobCtx.firestore(), 'custom_frames', 'frame_paid_1'), {
      favoriteCount: 0,
    })
  );
});
