package com.example

import android.content.Context
import android.util.Base64
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.TxlRepository
import com.example.data.local.PreferencesStore
import com.example.data.local.TxlDatabase
import com.example.model.AppRank
import com.example.model.AvatarFrame
import com.example.model.CreatorRank
import com.example.model.CustomFrameStyle
import com.example.model.FrameSlotTier
import com.example.model.StoreFrameCategory
import com.example.model.UserProfile
import com.example.ui.screens.ExploreScreen
import com.example.ui.theme.TXLsocialTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: TxlDatabase
    private lateinit var repo: TxlRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, TxlDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = TxlRepository(context, db, PreferencesStore(context))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read app name from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("NexoLive", appName)
    }

    @Test
    fun `no bots exist initially and only verified primary owner email becomes owner`() = runTest {
        repo.initializeSeedData()

        val allInitialUsers = repo.observeAllUsersList().first()
        val initialCommunities = repo.observeCommunities("").first()
        val initialBottles = repo.observeDriftingBottles().first()
        assertTrue("No debe haber bots al iniciar", allInitialUsers.isEmpty())
        assertTrue("No debe haber salas falsas al iniciar", initialCommunities.isEmpty())
        assertTrue("No debe haber botellas falsas al iniciar", initialBottles.isEmpty())

        // Non-owner account starts with 100 coins and NONE rank
        val member = repo.registerLocalAccountForTest("OtroDispositivo", "otro@txl.test").getOrThrow()
        assertEquals(AppRank.NONE, member.appRank)
        assertFalse(member.isDeveloper)
        assertFalse(member.canManageVerifications)
        assertEquals(100, member.coinsCount)

        // Spoofed unverified owner email must NOT receive Owner privileges
        val unverifiedSpoof = repo.registerLocalAccountForTest("ImpostorOwner", "fake_owner@txl.test", emailVerified = false).getOrThrow()
        assertEquals(AppRank.NONE, unverifiedSpoof.appRank)
        assertFalse(unverifiedSpoof.isPrimaryVerifiedOwner)

        // Verified primary owner email gets Owner + infinite coins
        val owner = repo.registerLocalAccountForTest("AdminReal", TxlRepository.PRIMARY_OWNER_EMAIL, emailVerified = true).getOrThrow()
        assertEquals(AppRank.OWNER, owner.appRank)
        assertTrue(owner.isDeveloper)
        assertTrue(owner.isPrimaryVerifiedOwner)
        assertTrue(owner.hasInfiniteCoins)

        assertTrue(repo.assignAppRank(owner.id, member.id, AppRank.MODERATOR).isSuccess)
        assertTrue(repo.assignAppRank(member.id, owner.id, AppRank.NONE).isFailure)
        assertEquals(AppRank.MODERATOR, repo.getUserById(member.id)?.appRank)
    }

    @Test
    fun `strict coin economy prevents non-owner refills and allows owner gifts plus donor progression`() = runTest {
        val owner = repo.registerLocalAccountForTest("DueñoNexo", TxlRepository.PRIMARY_OWNER_EMAIL).getOrThrow()
        val alice = repo.registerLocalAccountForTest("AliceUser", "alice@txl.test").getOrThrow()
        val bob = repo.registerLocalAccountForTest("BobCreator", "bob@txl.test").getOrThrow()

        assertEquals(100, alice.coinsCount)
        assertTrue(repo.claimFreeCoins(alice.id, 1000).isFailure)
        assertTrue(repo.claimFreeCoins(owner.id, 1000).isSuccess)

        // Non-owner cannot gift arbitrary coins; Owner can grant coins to anyone
        assertTrue(repo.grantCoinsToUser(alice.id, bob.id, 500).isFailure)
        assertTrue(repo.grantCoinsToUser(owner.id, alice.id, 500).isSuccess)
        assertEquals(600, repo.getUserById(alice.id)?.coinsCount)

        val cid = repo.createCommunity(bob.id, "Sala Global", "Música", "Bienvenidos", "music", null).getOrThrow()
        val giftOutcome = repo.sendRoomGift(cid, "VOICE", alice.id, bob.id, "money_gun", 1, null).getOrThrow()
        assertEquals(500, giftOutcome.event.coinCost)

        // Alice spent 500, leveled up from 1 -> 3 (Silver Elite +120 coin bonus) => 600 - 500 + 120 = 220 coins
        val aliceAfter = repo.getUserById(alice.id)!!
        assertEquals(220, aliceAfter.coinsCount)
        assertEquals(500, aliceAfter.donorExp)
        assertEquals(3, aliceAfter.donorTier.level) // 500 EXP >= Silver Elite (350)

        // Bob received 500 coins => 100 + 500 = 600
        val bobAfter = repo.getUserById(bob.id)!!
        assertEquals(600, bobAfter.coinsCount)
        assertEquals(1, bobAfter.giftsReceivedCount)
    }

    @Test
    fun `tienda de creaciones enforces 6-tier cupos, publication fees, and 80-20 sale payout`() = runTest {
        val owner = repo.registerLocalAccountForTest("OwnerTienda", TxlRepository.PRIMARY_OWNER_EMAIL).getOrThrow()
        val creator = repo.registerLocalAccountForTest("ArtistGlobal", "artist@txl.test").getOrThrow()
        val buyer = repo.registerLocalAccountForTest("BuyerGlobal", "buyer@txl.test").getOrThrow()

        // Fund creator & buyer via Owner gift
        repo.grantCoinsToUser(owner.id, creator.id, 1000).getOrThrow() // total 1100
        repo.grantCoinsToUser(owner.id, buyer.id, 500).getOrThrow() // total 600

        // Publish 10 free designs to fill initial 10/10 cupo
        val createdIds = mutableListOf<String>()
        for (i in 1..10) {
            val res = repo.publishStudioFrame(
                creatorId = creator.id,
                title = "Marco #$i",
                primaryColorHex = 0xFF9D4EDD,
                secondaryColorHex = 0xFFFF4EC2,
                stylePattern = CustomFrameStyle.RING_GLOW,
                thicknessDp = 4f,
                hasSparkles = true,
                priceCoins = 0
            )
            assertTrue("Design $i should succeed", res.isSuccess)
            createdIds += res.getOrThrow().id
        }

        // 11th must fail with cupo full
        val blocked = repo.publishStudioFrame(
            creatorId = creator.id,
            title = "Marco #11",
            primaryColorHex = 0xFF4CC9F0,
            secondaryColorHex = 0xFFFF4EC2,
            stylePattern = CustomFrameStyle.DOUBLE_ORBIT,
            thicknessDp = 4f,
            hasSparkles = true,
            priceCoins = 0
        )
        assertTrue(blocked.isFailure)

        // Unlock Nivel 1 (10 -> 20 spaces for 150 coins => 1100 - 150 = 950)
        val tierUnlocked = repo.unlockNextFrameSlotTier(creator.id).getOrThrow()
        assertEquals(FrameSlotTier.LEVEL_1, tierUnlocked)
        assertEquals(20, repo.getUserById(creator.id)?.effectiveMaxFrameSlots)
        assertEquals(950, repo.getUserById(creator.id)?.coinsCount)

        // Publish paid featured Store frame (50 publish + 200 highlight = 250 coins => 950 - 250 = 700)
        val paidFrame = repo.publishStudioFrame(
            creatorId = creator.id,
            title = "Aura Cósmica VIP",
            primaryColorHex = 0xFFFFD166,
            secondaryColorHex = 0xFFFF4EC2,
            stylePattern = CustomFrameStyle.STAR_BURST,
            thicknessDp = 5f,
            hasSparkles = true,
            priceCoins = 200,
            description = "Brillo dorado para directos",
            category = StoreFrameCategory.AURA.label,
            isFeatured = true,
            chargeStoreFee = true
        ).getOrThrow()
        assertEquals(200, paidFrame.priceCoins)
        assertTrue(paidFrame.isFeatured)
        assertEquals(700, repo.getUserById(creator.id)?.coinsCount)

        // Buyer favorites & purchases the frame for 200 -> 80% (160) to creator, 20% (40) platform fee
        assertTrue(repo.toggleFavoriteFrame(buyer.id, paidFrame.id).getOrThrow())
        assertTrue(repo.buyCustomFrame(buyer.id, paidFrame.id).isSuccess)

        val buyerAfter = repo.getUserById(buyer.id)!!
        val creatorAfter = repo.getUserById(creator.id)!!
        assertEquals(400, buyerAfter.coinsCount) // 600 - 200
        assertEquals(paidFrame.id, buyerAfter.avatarFrameId)
        assertTrue(paidFrame.id in buyerAfter.favoriteFrameIds)
        assertEquals(860, creatorAfter.coinsCount) // 700 + 160 (80% of 200)
        assertEquals(1, creatorAfter.creatorSalesCount)
        assertEquals(160, creatorAfter.creatorEarningsCoins)
        assertEquals(CreatorRank.RANK_1, creatorAfter.creatorRank)
    }

    @Test
    fun `rooms can be customized and deleted and studio frames plus posts work end to end`() = runTest {
        val owner = repo.registerLocalAccountForTest("CreadorUnico", "creador@txl.test").getOrThrow()
        val guest = repo.registerLocalAccountForTest("InvitadoReal", "invitado@txl.test").getOrThrow()

        val frame = repo.publishStudioFrame(
            creatorId = guest.id,
            title = "Galaxia Libre",
            primaryColorHex = 0xFF9D4EDD,
            secondaryColorHex = 0xFFFF4EC2,
            stylePattern = CustomFrameStyle.DOUBLE_ORBIT,
            thicknessDp = 5f,
            hasSparkles = true
        ).getOrThrow()
        assertEquals("STUDIO", frame.frameType)

        repo.updateProfileCustomization(
            userId = guest.id,
            username = guest.username,
            bio = "Hola TXL",
            customStatus = "En linea",
            frameId = frame.id,
            avatarUri = null,
            presetAvatarKey = "gaming"
        )
        assertEquals(frame.id, repo.getUserById(guest.id)?.avatarFrameId)

        val cid = repo.createCommunity(
            creatorId = guest.id,
            name = "Sala Cyber",
            category = "Tech",
            description = "Sala de prueba real",
            coverKey = "gaming",
            backgroundUri = "/tmp/custom_bg.jpg"
        ).getOrThrow()

        assertEquals("/tmp/custom_bg.jpg", repo.getCommunityById(cid)?.backgroundUri)

        val post = repo.createPost(guest.id, cid, "Hola comunidad NexoLive!", null).getOrThrow()
        repo.likePost(post.id)
        assertTrue(repo.addPostComment(post.id, owner.id, "Excelente post").isSuccess)

        val targetPost = repo.observePosts().first().firstOrNull { it.id == post.id }
        assertNotNull(targetPost)
        assertEquals(1, targetPost?.likesCount)
        assertEquals(1, targetPost?.commentsCount)

        assertTrue(repo.deleteCommunity(cid, guest.id).isSuccess)
        assertNull(repo.getCommunityById(cid))

        val postAfterRoomDel = repo.observePosts().first().firstOrNull { it.id == post.id }
        assertNotNull(postAfterRoomDel)
        assertNull(postAfterRoomDel?.communityId)
        assertTrue(repo.deletePost(post.id, guest.id).isSuccess)
        assertTrue(repo.observePosts().first().none { it.id == post.id })
    }

    @Test
    fun `owner keeps verification permissions after changing own badge and non-owner never gets dev permissions`() = runTest {
        val dev = repo.registerLocalAccountForTest("DevOwner", TxlRepository.PRIMARY_OWNER_EMAIL, emailVerified = true).getOrThrow()
        val secondary = repo.registerLocalAccountForTest("CuentaDos", "dos@txl.test", emailVerified = true).getOrThrow()

        assertTrue(dev.isDeveloper)
        assertTrue(dev.canManageVerifications)
        assertFalse(secondary.isDeveloper)
        assertFalse(secondary.canManageVerifications)

        assertTrue(repo.assignAppRank(dev.id, dev.id, AppRank.MODERATOR).isSuccess)
        assertEquals(AppRank.MODERATOR, repo.getUserById(dev.id)?.appRank)
        assertTrue(repo.getUserById(dev.id)?.canManageVerifications == true)

        assertTrue(repo.assignAppRank(dev.id, dev.id, AppRank.NONE).isSuccess)
        assertEquals(AppRank.NONE, repo.getUserById(dev.id)?.appRank)
        assertTrue(repo.getUserById(dev.id)?.canManageVerifications == true)

        assertTrue(repo.assignAppRank(dev.id, secondary.id, AppRank.INFLUENCER).isSuccess)
        assertEquals(AppRank.INFLUENCER, repo.getUserById(secondary.id)?.appRank)
        assertTrue(repo.assignAppRank(dev.id, dev.id, AppRank.OWNER).isSuccess)
        assertEquals(AppRank.OWNER, repo.getUserById(dev.id)?.appRank)

        assertTrue(repo.assignAppRank(secondary.id, dev.id, AppRank.NONE).isFailure)
    }

    @Test
    fun `explore user search filters registered users and opens profile sheet`() {
        val alice = UserProfile(
            id = "u_alice",
            username = "LuciaNeon",
            email = "lucia@txl.test",
            bio = "Creadora de arte synthwave",
            customStatus = "Escuchando beats",
            avatarFrame = AvatarFrame.NEON_PULSE,
            avatarFrameId = AvatarFrame.NEON_PULSE.id,
            avatarUri = null,
            presetAvatarKey = "anime",
            communitiesCount = 2,
            followersCount = 14,
            postsCount = 3,
            appRank = AppRank.INFLUENCER,
            isDeveloper = false
        )
        val bob = UserProfile(
            id = "u_bob",
            username = "CarlosGamer",
            email = "carlos@txl.test",
            bio = "Torneos nocturnos",
            customStatus = "En partida",
            avatarFrame = AvatarFrame.CYBER_PRISM,
            avatarFrameId = AvatarFrame.CYBER_PRISM.id,
            avatarUri = null,
            presetAvatarKey = "gaming",
            communitiesCount = 1,
            followersCount = 5,
            postsCount = 1,
            appRank = AppRank.NONE,
            isDeveloper = false
        )

        composeRule.setContent {
            TXLsocialTheme {
                ExploreScreen(
                    communities = emptyList(),
                    allUsers = listOf(alice, bob),
                    currentUserId = bob.id,
                    currentUserRank = AppRank.NONE,
                    currentUserIsDeveloper = false,
                    onOpenVoiceRoom = {},
                    onOpenGroupChat = {},
                    onToggleJoin = {},
                    onCreateCommunity = { _, _, _, _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("explore_user_search_input").performTextInput("Lucia")
        composeRule.onNodeWithTag("explore_user_card_u_alice").assertIsDisplayed()
        composeRule.onNodeWithTag("explore_user_card_u_bob").assertDoesNotExist()

        composeRule.onNodeWithTag("explore_user_card_u_alice").performClick()
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Creadora de arte synthwave").assertExists()
    }

    @Test
    fun `group chat component renders stylized dark bubbles and sends messages`() {
        val streamer = UserProfile(
            id = "u_streamer",
            username = "KuroIRL",
            email = "kuro@nexolive.test",
            bio = "IRL Tokio",
            customStatus = "En vivo desde Shibuya",
            avatarFrame = AvatarFrame.NEON_PULSE,
            avatarFrameId = AvatarFrame.NEON_PULSE.id,
            avatarUri = null,
            presetAvatarKey = "irl",
            communitiesCount = 1,
            followersCount = 1200,
            postsCount = 4,
            appRank = AppRank.INFLUENCER
        )
        val viewer = UserProfile(
            id = "u_viewer",
            username = "MaxGamer",
            email = "max@nexolive.test",
            bio = "FPS player",
            customStatus = "Ranked",
            avatarFrame = AvatarFrame.CYBER_PRISM,
            avatarFrameId = AvatarFrame.CYBER_PRISM.id,
            avatarUri = null,
            presetAvatarKey = "fps",
            communitiesCount = 1,
            followersCount = 24,
            postsCount = 1,
            appRank = AppRank.NONE
        )
        val initialMessage = com.example.model.ChatMessage(
            id = 101L,
            communityId = "c_1",
            channelType = "GROUP",
            sender = streamer,
            senderRole = com.example.model.CommunityRole.ADMIN,
            content = "¡Bienvenidos al directo IRL!",
            reactionCount = 4,
            timestampMillis = System.currentTimeMillis()
        )
        var sentText: String? = null

        composeRule.setContent {
            TXLsocialTheme {
                val inputState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
                com.example.ui.components.GroupChatPanel(
                    messages = listOf(initialMessage),
                    currentUserId = viewer.id,
                    myRole = com.example.model.CommunityRole.MEMBER,
                    showUserStatus = true,
                    inputText = inputState.value,
                    onInputTextChange = { inputState.value = it },
                    replyingTo = null,
                    onSetReplyingTo = {},
                    onSendMessage = { content, _ -> sentText = content },
                    onReactMessage = {},
                    onDeleteMessage = {},
                    onSelectUser = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("chat_bubble_101").assertIsDisplayed()
        composeRule.onNodeWithText("¡Bienvenidos al directo IRL!").assertIsDisplayed()
        composeRule.onNodeWithTag("group_chat_input").performTextInput("GG buen directo!")
        composeRule.onNodeWithTag("group_chat_send_button").performClick()
        assertEquals("GG buen directo!", sentText)
    }

    @Test
    fun `audio messages can be saved, sent, observed, and cleaned up on deletion`() = runTest {
        val dev = repo.registerLocalAccountForTest("AudioDev", TxlRepository.PRIMARY_OWNER_EMAIL).getOrThrow()
        val cid = repo.createCommunity(dev.id, "Sala Audio", "Música", "Notas de voz", "music", null).getOrThrow()

        val fakeAudioDataUrl = "data:audio/webm;base64," + Base64.encodeToString("TXL_FAKE_AUDIO_BYTES".toByteArray(), Base64.NO_WRAP)
        val audioPath = repo.saveBase64AudioToInternalStorage(fakeAudioDataUrl).getOrThrow()
        assertTrue(java.io.File(audioPath).exists())

        val sendRes = repo.sendAudioMessage(
            communityId = cid,
            channelType = "VOICE",
            senderId = dev.id,
            audioUri = audioPath,
            durationMs = 5400L
        )
        assertTrue(sendRes.isSuccess)

        val voiceMessages = repo.observeMessages(cid, "VOICE").first()
        assertEquals(1, voiceMessages.size)
        val msg = voiceMessages.first()
        assertTrue(msg.isAudioMessage)
        assertEquals(5400L, msg.audioDurationMs)
        assertEquals(audioPath, msg.audioUri)
        assertNotNull(repo.getMessageById(msg.id))

        assertTrue(repo.deleteMessageById(cid, dev.id, msg.id).isSuccess)
        assertNull(repo.getMessageById(msg.id))
        assertFalse(java.io.File(audioPath).exists())
    }

    @Test
    fun `only verified owner can ban and unban users and banned accounts lose write access until unbanned`() = runTest {
        val owner = repo.registerLocalAccountForTest("OwnerPrincipal", TxlRepository.PRIMARY_OWNER_EMAIL).getOrThrow()
        val normalA = repo.registerLocalAccountForTest("Infractor", "infractor@txl.test").getOrThrow()
        val normalB = repo.registerLocalAccountForTest("OtroUsuario", "otro2@txl.test").getOrThrow()

        assertTrue(repo.banUser(normalB.id, normalA.id, "Spam").isFailure)
        assertTrue(repo.banUser(owner.id, owner.id, "Auto ban").isFailure)

        val cid = repo.createCommunity(owner.id, "Sala Principal", "Chat", "Bienvenidos", "anime", null).getOrThrow()
        repo.joinVoiceRoom(cid, normalA.id)
        assertTrue(repo.observeVoiceParticipants(cid).first().any { it.user.id == normalA.id })

        assertTrue(repo.banUser(owner.id, normalA.id, "Contenido ilegal prohibido").isSuccess)

        val bannedProfile = repo.getUserById(normalA.id)
        assertTrue(bannedProfile?.isBanned == true)
        assertEquals("Contenido ilegal prohibido", bannedProfile?.banReason)

        assertTrue(repo.observeVoiceParticipants(cid).first().none { it.user.id == normalA.id })
        assertTrue(repo.createPost(normalA.id, cid, "Intentando publicar", null).isFailure)
        assertTrue(repo.launchDriftingBottle(normalA.id, "Mood", "Intentando botella").isFailure)

        assertTrue(repo.unbanUser(normalB.id, normalA.id).isFailure)
        assertTrue(repo.unbanUser(owner.id, normalA.id).isSuccess)
        val unbanned = repo.getUserById(normalA.id)
        assertFalse(unbanned?.isBanned == true)
        assertNull(unbanned?.banReason)
        assertTrue(repo.createPost(normalA.id, cid, "De regreso respetando las normas", null).isSuccess)
    }
}
