package com.paperless.data.repository.base

import com.paperless.data.repository.AdminRepository
import com.paperless.data.repository.AgendaRepository
import com.paperless.data.repository.BulletinRepository
import com.paperless.data.repository.base.DataRepository
import com.paperless.data.repository.DeviceInfoRepository
import com.paperless.data.repository.DeviceMeetInfoRepository
import com.paperless.data.repository.DirectoryFileRepository
import com.paperless.data.repository.DirectoryRepository
import com.paperless.data.repository.DwonloadRepository
import com.paperless.data.repository.FunctionConfigRepository
import com.paperless.data.repository.InterfaceConfigRepository
import com.paperless.data.repository.MeetingInfoRepository
import com.paperless.data.repository.MemberDetailRepository
import com.paperless.data.repository.MemberPermissionRepository
import com.paperless.data.repository.NewVoteRepository
import com.paperless.data.repository.PeopleRepository
import com.paperless.data.repository.RoomRepository
import com.paperless.data.repository.SignInRepository
import com.paperless.data.repository.TableCardRepository
import com.paperless.data.repository.VoteRepository
import com.paperless.sdk.Call

object DataRepositoryManager {

    private val memberDetailRepo = MemberDetailRepository()
    private val deviceInfoRepo = DeviceInfoRepository()
    private val roomRepo = RoomRepository()
    private val meetingInfoRepo = MeetingInfoRepository()
    private val agendaRepo = AgendaRepository()
    private val bulletinRepo = BulletinRepository()
    private val directoryRepo = DirectoryRepository()
    private val voteRepo = VoteRepository()
    private val signInRepo = SignInRepository()
    private val adminRepo = AdminRepository()
    private val peopleRepo = PeopleRepository()
    private val memberPermissionRepo = MemberPermissionRepository()
    private val tableCardRepo = TableCardRepository()
    private val functionConfigRepo = FunctionConfigRepository()
    private val deviceMeetInfoRepo = DeviceMeetInfoRepository()
    private val newVoteRepo = NewVoteRepository()
    private val interfaceConfigRepo = InterfaceConfigRepository()
    private val dwonloadRepo = DwonloadRepository()
    private val directoryFileRepo = DirectoryFileRepository()

    private val allRepos: List<DataRepository> = listOf(
        memberDetailRepo, deviceInfoRepo, roomRepo, meetingInfoRepo,
        agendaRepo, bulletinRepo, directoryRepo, voteRepo, signInRepo,
        adminRepo, peopleRepo, memberPermissionRepo, tableCardRepo, functionConfigRepo,
        deviceMeetInfoRepo, newVoteRepo, dwonloadRepo, interfaceConfigRepo, directoryFileRepo
    )

    val memberDetailRepository = memberDetailRepo
    val deviceInfoRepository = deviceInfoRepo
    val roomRepository = roomRepo
    val meetingInfoRepository = meetingInfoRepo
    val agendaRepository = agendaRepo
    val bulletinRepository = bulletinRepo
    val directoryRepository = directoryRepo
    val directoryFileRepository = directoryFileRepo
    val voteRepository = voteRepo
    val signInRepository = signInRepo
    val adminRepository = adminRepo
    val peopleRepository = peopleRepo
    val memberPermissionRepository = memberPermissionRepo
    val tableCardRepository = tableCardRepo
    val functionConfigRepository = functionConfigRepo
    val deviceMeetInfoRepository = deviceMeetInfoRepo
    val newVoteRepository = newVoteRepo
    val interfaceConfigRepository = interfaceConfigRepo
    val dwonloadRepository = dwonloadRepo

    fun init() {
        Call.setDataChangeCallback(CallbackDispatcher)
        dwonloadRepository.onDownloadCompleted = { userstr, filePath ->
            interfaceConfigRepository.updateFilePath(userstr, filePath)
        }
    }

    fun refreshAll() {
        allRepos.forEach { it.query() }
    }

    fun refresh(type: Int) {
        allRepos.find { it.type == type }?.query()
    }

    fun destroy() {
        allRepos.forEach { it.destroy() }
        Call.setDataChangeCallback(null)
    }
}