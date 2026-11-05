# Gson 反射解析的数据模型：字段名即 json 的 key，混淆后会解析失败
# 例：BaseJni.queryMeetingCreatetime 返回的 MeetCreateTimeJson
-keep class com.paperless.data.json.** { *; }
