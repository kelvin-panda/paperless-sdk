package com.paperless.bus

/**
 *  @author : Administrator
 *  @date : 2023/10/25 18:49
 *  @description :
 */
class BusType {
    companion object {
        private const val base_value = 10086
        const val apply_record_permission_successful = base_value + 0
        const val capture_start = base_value + 1
        const val capture_stop = base_value + 2
        const val receive_message = base_value + 3
        const val clear_unread_message = base_value + 4
        const val update_room_bg = base_value + 5
        const val push_file = base_value + 6
        const val after_screenshot = base_value + 7
        const val draw_screenshot = base_value + 8

        /**
         * 会议界面切换到更多功能界面
         */
        const val toggle_more_function = base_value + 9
        const val yuv_data = base_value + 10
        const val update_bulletin_logo = base_value + 11
        const val update_bulletin_bg = base_value + 12
        const val hide_floating_button = base_value + 13
        const val chat_function_toggle = base_value + 14
        const val serve_function_toggle = base_value + 15
        const val update_sub_bg = base_value + 16
        const val download_agenda_file_finish = base_value + 17
        const val update_logo = base_value + 18
        const val update_main_bg = base_value + 19
        const val inform_disconnected_dialog = base_value + 20
        const val inform_silent_download = base_value + 21
        const val upload_projection_bg = base_value + 22
        const val upload_projection_logo = base_value + 23
        const val download_table_bg_finish = base_value + 24
        const val toggle_offline_meeting = base_value + 25
        const val inform_monitor_network = base_value + 26
        const val open_file = base_value + 27
        const val toggle_member_function = base_value + 28
        const val fps = base_value + 29
        const val switch_meeting = base_value + 30
    }
}