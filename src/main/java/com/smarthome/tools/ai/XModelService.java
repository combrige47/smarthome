package com.smarthome.tools.ai;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.smarthome.tools.mqtt.entity.MqttEntity;
import com.smarthome.tools.mqtt.repository.MqttDataRepository;
import com.smarthome.tools.mqtt.service.DataCache;
import com.smarthome.tools.mqtt.service.MqttMessageSender;
import com.vladmihalcea.hibernate.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class XModelService {
    private final DataCache mqttdata;
    private final MqttMessageSender mqttPublishService;

    // AI 配置参数（从配置文件注入）
    @Value("${ai.api.password}")
    private String apiPassword;
    @Value("${ai.user.id:10284711用户}")
    private String userId;
    private static final String AI_API_URL = "https://spark-api-open.xf-yun.com/v2/chat/completions";
    private static final String CHARSET = StandardCharsets.UTF_8.name();

    // 构造函数：注入所有依赖（Spring 自动装配）
    public XModelService(DataCache mqttdata, MqttMessageSender mqttPublishService) {
        this.mqttdata = mqttdata;
        this.mqttPublishService = mqttPublishService;
    }

    /**
     * 统一入口：接收用户指令，AI 分类后执行对应功能
     * @param voiceText 用户指令（如“查1号灯状态”“发布客厅温度主题 25℃”“打开风扇”）
     * @return 功能执行结果（如数据内容、发布状态、控制指令）
     */
    public String processAiRequest(String voiceText) {
        try {
            // 1. 调用 AI：识别指令类型，输出“类型|参数JSON”格式（如“数据查询|{"deviceCode":"l1"}”）
            String aiResult = getAiClassificationResult(voiceText);
            System.out.println("AI 指令分类结果：" + aiResult);

            // 2. 解析 AI 输出：拆分“功能类型”和“参数”
            AiFunctionType functionType = parseFunctionType(aiResult);
            JSONObject functionParams = parseFunctionParams(aiResult);

            // 3. 按类型路由到对应功能
            return switch (functionType) {
                case DATA_QUERY -> handleDataQuery(functionParams);    // 处理“获取特定数据”
                case DEVICE_CONTROL -> handleDeviceControl(voiceText); // 处理“设备控制”（复用原逻辑）
                case UNKNOWN -> null;
            };
        } catch (Exception e) {
            e.printStackTrace();
            return "处理失败：" + e.getMessage().replaceAll("\"", "'");
        }
    }

    // ------------------------------ 1. AI 指令分类：让 AI 识别指令类型 ------------------------------
    /**
     * 调用讯飞 AI，让其按固定格式输出“功能类型|参数JSON”
     */
    private String getAiClassificationResult(String voiceText) throws Exception {
        // 提示词：强制 AI 按“类型|参数”格式输出，避免多余内容
        String prompt = String.format(
                "【核心任务】：仅完成两步：1. 判断用户指令的功能类型；2. 按固定格式输出结果，无任何解释、无多余内容。\n" +
                        "【功能类型定义】：\n" +
                        "1. 数据查询：用户需获取设备状态或环境数据（如查卧室灯状态、查客厅温度、查厨房燃气浓度）；\n" +
                        "2. 设备控制：用户需操作设备开关/档位（如开卧室2号灯、关卫生间排风扇、调淋浴间水泵档位）；\n" +
                        "【关键设备信息（必须参考）】：\n" +
                        "- 设备编码（对应topic）：卧室=bedroom、卫生间=toilet、厨房=kitchen、门禁系统=access_control、客厅=living_room、淋浴间=shower_room_status；\n" +
                        "- 各设备可操作/查询字段：\n" +
                        "  卧室（bedroom）：l0（0号灯）、l1（1号灯）、l2（2号灯）、fan（风扇）、tv（电视）；\n" +
                        "  卫生间（toilet）：toilet_state（使用状态）、light_state（灯光）、fan_state（排风扇）；\n" +
                        "  厨房（kitchen）：alarmbell（燃气报警器）、gas（燃气浓度）、fan_status（厨房风扇）；\n" +
                        "  客厅（living_room）：fire_alarm（火灾报警器）、fire_state（火灾状态）、temp（温度）、hum（湿度）；\n" +
                        "  淋浴间（shower_room_status）：pump_level（水泵档位）、status（运行状态）；\n" +
                        "  门禁系统（access_control）：isHuman（人体检测）、isCall（呼叫状态）、owner（主人模式）；\n" +
                        "  大灯(led_1_status) :  state(状态)、level(亮度)；\n"+
                        "  水泵状态(water_pump_1_status) :  pump_level(水泵强度)、mode(水泵模式)、moisture(湿度)\n"+
                        "【输出格式要求（必须严格遵守）】：\n" +
                        "格式1：数据查询→\"数据查询|{\"target\":\"查询目标\",\"deviceCode\":\"设备编码\",\"field\":\"查询字段（可选）\"}\"\n" +
                        "格式2：设备控制→\"设备控制|{\"content\":\"原用户指令\",\"deviceCode\":\"设备编码\",\"field\":\"操作字段\",\"action\":\"操作（开/关/调档位等）\"}\"\n" +
                        "【参数说明】：\n" +
                        "- 数据查询：\n" +
                        "  - target：需包含“设备名+字段”（如“卧室2号灯状态”“客厅温度”“厨房燃气浓度”）；\n" +
                        "  - deviceCode：必选，对应上述设备编码（如查卧室灯→deviceCode=bedroom）；\n" +
                        "  - field：可选，需查询的具体字段（如查卧室l2→field=l2，查客厅所有→field留空）；\n" +
                        "- 设备控制：\n" +
                        "  - content：原用户指令（保留完整语义，如“打开卧室l2灯”）；\n" +
                        "  - deviceCode：必选，操作的设备编码（如操作卫生间→deviceCode=toilet）；\n" +
                        "  - field：必选，操作的具体字段（如关卫生间排风扇→field=fan_state）；\n" +
                        "  - action：必选，操作类型（如开/关/调1档/调2档，示例：开→action=开，调水泵到1档→action=调1档）；\n" +
                        "【正确示例（必须参考）】：\n" +
                        "1. 用户说“查卧室l2灯状态”→输出\"数据查询|{\"target\":\"卧室2号灯状态\",\"deviceCode\":\"bedroom\",\"field\":\"l2\"}\"\n" +
                        "2. 用户说“查客厅温度和湿度”→输出\"数据查询|{\"target\":\"客厅温度和湿度\",\"deviceCode\":\"living_room\",\"field\":\"temp,hum\"}\"\n" +
                        "3. 用户说“打开卫生间灯光”→输出\"设备控制|{\"content\":\"打开卫生间灯光\",\"deviceCode\":\"toilet\",\"field\":\"light_state\",\"action\":\"开\"}\"\n" +
                        "4. 用户说“调淋浴间水泵到1档”→输出\"设备控制|{\"content\":\"调淋浴间水泵到1档\",\"deviceCode\":\"shower_room_status\",\"field\":\"pump_level\",\"action\":\"调1档\"}\"\n" +
                        "5. 用户说“查所有设备状态”→输出\"数据查询|{\"target\":\"所有设备状态\",\"deviceCode\":\"\",\"field\":\"\"}\"\n" +
                        "【错误禁止】：\n" +
                        "- 禁止省略deviceCode（如查卫生间→必须写deviceCode=toilet，不能写“卫生间”）；\n" +
                        "- 禁止字段名错误（如卧室2号灯→field=l2，不能写“2号灯”）；\n" +
                        "- 禁止格式错误（必须用“|”分隔，JSON字段用双引号，不能用单引号）；\n" +
                        "用户指令：%s",
                voiceText
        );

        // 构造 AI 请求参数（符合讯飞 API 规范）
        JSONObject aiReqJson = new JSONObject();
        aiReqJson.put("user", userId);
        aiReqJson.put("model", "x1");
        aiReqJson.put("temperature", 0.0); // 0.0=无随机性，确保格式正确
        aiReqJson.put("top_k", 1);
        aiReqJson.put("stream", false);
        aiReqJson.put("max_tokens", 200);

        // 构造 messages 数组
        JSONArray messages = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);
        aiReqJson.put("messages", messages);

        // 发送 HTTP 请求到讯飞 API，返回 AI 结果
        HttpURLConnection connection = createAiHttpConnection(aiReqJson.toString());
        return readAiResponse(connection);
    }

    // ------------------------------ 2. 解析 AI 输出：拆分类型和参数 ------------------------------
    /**
     * 从 AI 结果中提取“功能类型”（如从“数据查询|{...}”中取“数据查询”）
     */
    private AiFunctionType parseFunctionType(String aiResult) {
        if (!aiResult.contains("|")) return AiFunctionType.UNKNOWN;
        String typeName = aiResult.split("\\|")[0].trim();
        return AiFunctionType.matchByTypeName(typeName);
    }

    /**
     * 从 AI 结果中提取“参数 JSON”（如从“数据查询|{"deviceCode":"l1"}”中取 JSON）
     */
    private JSONObject parseFunctionParams(String aiResult) {
        if (!aiResult.contains("|")) return new JSONObject();
        try {
            String jsonStr = aiResult.split("\\|")[1].trim();
            return new JSONObject(jsonStr);
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject(); // 解析失败返回空 JSON
        }
    }

    // ------------------------------ 3. 功能实现：按类型处理 ------------------------------
    /**
     * 处理“获取特定数据”：目前支持设备状态查询，可扩展环境温度等
     */
    private String handleDataQuery(JSONObject params) {
        String target = params.getStr("target", "");       // 查询目标（如“卧室2号灯状态”）
        String deviceCode = params.getStr("deviceCode", ""); // 设备编码（如bedroom、toilet）
        String field = params.getStr("field", "");         // 要查询的字段（如l2、temp，多字段用逗号分隔）

        // 处理所有数据查询场景（不再局限于"设备状态"关键词，适配更灵活的target）
        // 1. 场景1：查询所有设备状态（deviceCode为空，field为空）
        if (deviceCode.isEmpty() && field.isEmpty()) {
            Map<String, MqttEntity> allDeviceMap = mqttdata.getAlldata();
            if (allDeviceMap.isEmpty()) {
                return "暂无任何设备数据，请检查 MQTT 连接是否正常";
            }

            StringBuilder result = new StringBuilder("所有设备当前状态汇总：\n");
            for (Map.Entry<String, MqttEntity> entry : allDeviceMap.entrySet()) {
                String devCode = entry.getKey();
                MqttEntity mqttEntity = entry.getValue();
                // 调用通用解析方法处理所有设备
                result.append(parseDeviceStatusByTopic(devCode, mqttEntity)).append("\n");
            }
            return result.toString().trim();
        }

        // 2. 场景2：查询特定设备的所有字段（deviceCode不为空，field为空）
        else if (!deviceCode.isEmpty() && field.isEmpty()) {
            Map<String, MqttEntity> allDeviceMap = mqttdata.getAlldata();
            MqttEntity targetDevice = allDeviceMap.get(deviceCode);
            if (targetDevice == null) {
                return "暂无“" + deviceCode + "”的数据";
            }
            // 解析该设备的所有字段
            return parseDeviceStatusByTopic(deviceCode, targetDevice);
        }

        // 3. 场景3：查询特定设备的特定字段（deviceCode和field都不为空）
        else if (!deviceCode.isEmpty() && !field.isEmpty()) {
            Map<String, MqttEntity> allDeviceMap = mqttdata.getAlldata();
            MqttEntity targetDevice = allDeviceMap.get(deviceCode);
            if (targetDevice == null) {
                return "未找到编码为“" + deviceCode + "”的设备";
            }

            JSONObject payload = targetDevice.getPayload();
            String devName = getDeviceNameByTopic(deviceCode); // 从设备编码获取中文名称
            String updateTime = targetDevice.getTimestampString();
            StringBuilder result = new StringBuilder(devName +"的状态：\n");

            // 处理多字段（用逗号分割）
            String[] fields = field.split(",");
            for (String f : fields) {
                f = f.trim(); // 去除空格
                // 根据设备编码和字段名解析具体值（调用对应设备的字段解析逻辑）
                String fieldValue = parseSpecificField(deviceCode, payload, f);
                result.append("  - ").append(getFieldDisplayName(deviceCode, f)).append("：").append(fieldValue).append("\n");
            }
            result.append("更新时间：").append(updateTime);
            return result.toString().trim();
        }

        // 4. 无效场景（deviceCode为空但field不为空，无法定位设备）
        else {
            return "查询参数错误：请指定具体设备（如bedroom）后再查询字段";
        }
    }

    /**
     * 解析特定设备的特定字段值（按设备类型适配）
     * @param deviceCode 设备编码（如bedroom）
     * @param payload 设备的payload数据
     * @param field 要查询的字段（如l2、gas）
     * @return 字段的中文描述+值
     */
    private String parseSpecificField(String deviceCode, JSONObject payload, String field) {
        // 根据设备编码和字段名，返回对应的值（结合状态转换方法）
        switch (deviceCode) {
            case "bedroom":
                switch (field) {
                    case "l0": return convertLightFanStatus(payload.getInt(field, -1));
                    case "l1": return convertLightFanStatus(payload.getInt(field, -1));
                    case "l2": return convertLightFanStatus(payload.getInt(field, -1));
                    case "fan": return convertLightFanStatus(payload.getInt(field, -1));
                    case "tv": return convertTvStatus(payload.getInt(field, -1));
                    default: return "未知字段（" + field + "）";
                }
            case "toilet":
                switch (field) {
                    case "toilet_state": return convertToiletState(payload.getInt(field, -1));
                    case "light_state": return convertLightFanStatus(payload.getInt(field, -1));
                    case "fan_state": return convertLightFanStatus(payload.getInt(field, -1));
                    default: return "未知字段（" + field + "）";
                }
            case "kitchen":
                switch (field) {
                    case "alarmbell": return convertAlarmStatus(payload.getInt(field, -1));
                    case "gas": return payload.getInt(field, -1) + " ppm";
                    case "fan_status": return convertLightFanStatus(payload.getInt(field, -1));
                    default: return "未知字段（" + field + "）";
                }
            case "access_control":
                switch (field) {
                    case "isHuman": return convertHumanDetectStatus(payload.getInt(field, -1));
                    case "isCall": return convertCallStatus(payload.getInt(field, -1));
                    case "owner": return convertOwnerModeStatus(payload.getInt(field, -1));
                    default: return "未知字段（" + field + "）";
                }
            case "living_room":
                switch (field) {
                    case "fire_alarm": return convertAlarmStatus(payload.getInt(field, -1));
                    case "fire_state": return convertFireState(payload.getInt(field, -1));
                    case "temp": return payload.getDouble(field, -1.0) + " ℃";
                    case "hum": return payload.getDouble(field, -1.0) + " %";
                    default: return "未知字段（" + field + "）";
                }
            case "shower_room_status":
                switch (field) {
                    case "pump_level": return payload.getInt(field, -1) + " 档";
                    case "status": return convertShowerStatus(payload.getStr(field, "unknown"));
                    default: return "未知字段（" + field + "）";
                }
            case "led_1_status":
                switch (field) {
                    case "state":
                        return convertLightFanStatus(payload.getInt(field, -1)); // 复用灯光状态转换
                    case "level":
                        int brightness = payload.getInt(field, -1);
                        return brightness == -1 ? "未知" : brightness + "（1-100）";
                    default:
                        return "未知字段（" + field + "）";
                }

                // 新增：灌溉水泵（water_pump_1_status）的字段解析
            case "water_pump_1_status":
                switch (field) {
                    case "pump_level":
                        int level = payload.getInt(field, -1);
                        return level == -1 ? "未知" : level + " 档（1-3）";
                    case "mode":
                        return convertPumpMode(payload.getStr(field, "unknown")); // 调用新增的模式转换方法
                    case "moisture":
                        int humidity = payload.getInt(field, -1);
                        return humidity == -1 ? "未知" : humidity + " %";
                    default:
                        return "未知字段（" + field + "）";
                }
            default:
                return "未知设备，无法解析字段";
        }
    }

    /**
     * 处理“设备控制”：复用原有逻辑，生成标准控制指令
     */
    private String handleDeviceControl(String voiceText) throws Exception {
        // 原有的设备控制指令生成逻辑
        String prompt = String.format(
                "【核心指令】：仅将用户设备操作需求转为「设备主题|标准指令」格式，无任何多余内容（无解释、无备注）。\n" +
                        "【前置规则】：\n" +
                        "1. 先识别用户操作的「目标设备」，匹配唯一对应的「MQTT主题」；\n" +
                        "2. 再根据设备类型，按规则生成「标准指令（payload）」；\n" +
                        "3. 最终输出格式：「设备主题|标准指令」（仅这一项内容，无其他字符）。\n" +
                        "\n" +
                        "【设备-主题-指令规则映射表】（必须严格遵守，不允许自定义）：\n" +
                        "1. 卧室设备（如卧室灯、风扇、电视）\n" +
                        "   - 对应主题：bedroom_cmd\n" +
                        "   - 设备映射：0号灯=l0、1号灯=l1、2号灯=l2、风扇=f、电视=TV\n" +
                        "   - 状态映射：开=1、关=0（电视特殊：开=on、关=off）\n" +
                        "   - 多设备操作：用英文逗号分隔（如操作多个设备，指令间用逗号拼接）\n" +
                        "\n" +
                        "2. 浴室水泵（shower_room）\n" +
                        "   - 对应主题：shower_room\n" +
                        "   - 操作类型：仅「调节挡位」\n" +
                        "   - 指令规则：直接输出数字0-3（0=关闭，1-3=对应挡位，必须在0-3范围内）\n" +
                        "\n" +
                        "3. 大灯（led_1）\n" +
                        "   - 对应主题：led_1\n" +
                        "   - 操作类型：仅「调节亮度」\n" +
                        "   - 指令规则：直接输出数字1-100（1=最低亮度，100=最高亮度，必须在1-100范围内）\n" +
                        "\n" +
                        "4. 灌溉水泵（water_pump_1）\n" +
                        "   - 对应主题：water_pump_1\n" +
                        "   - 操作类型：「模式切换」或「手动调挡」\n" +
                        "   - 指令规则：\n" +
                        "     - 切换自动模式：输出auto\n" +
                        "     - 切换手动模式：输出manual\n" +
                        "     - 手动模式下调挡：输出数字（如1、2、3，数字必须在1-3范围内）\n" +
                        "\n" +
                        "【正确示例】（必须参考格式，不允许偏离）：\n" +
                        "1. 用户说「打开卧室1号灯，关闭卧室电视」→ bedroom_cmd|l11,TVoff\n" +
                        "2. 用户说「把浴室水泵调到2档」→ shower_room|2\n" +
                        "3. 用户说「大灯亮度调到75」→ led_1|75\n" +
                        "4. 用户说「灌溉水泵切换手动模式」→ water_pump_1|manual\n" +
                        "5. 用户说「灌溉水泵切自动」→ water_pump_1|auto\n" +
                        "6. 用户说「卧室风扇开，浴室水泵关」→ 分两次输出（按设备主题拆分，每次1个主题）：\n" +
                        "   bedroom_cmd|f1\n" +
                        "   shower_room|0\n" +
                        "7. 用户说「灌溉水泵调到2档」要先切换至手动模式 →\n"+
                        "   water_pump_1|manual\n"+
                        "   water_pump_1|2\n"+
                        "\n" +
                        "【错误禁止】：\n" +
                        "1. 禁止省略主题（如仅输出「2」，必须带「shower_room_cmd|」）；\n" +
                        "2. 禁止指令超范围（如浴室水泵不能输出4，大灯不能输出0）；\n" +
                        "3. 禁止多主题混输（如同一行输出「bedroom_cmd|l11,shower_room_cmd|2」，需拆分两行）；\n" +
                        "4. 禁止多余字符（如「shower_room_cmd|2档」，不能带「档」字）；\n" +
                        "\n" +
                        "用户指令：%s",
                voiceText
        );

        // 构造 AI 请求
        JSONObject aiReqJson = new JSONObject();
        aiReqJson.put("user", userId);
        aiReqJson.put("model", "x1");
        aiReqJson.put("temperature", 0.2);
        aiReqJson.put("top_k", 2);
        aiReqJson.put("stream", false);
        aiReqJson.put("max_tokens", 50);

        JSONArray messages = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);
        aiReqJson.put("messages", messages);
        HttpURLConnection connection = createAiHttpConnection(aiReqJson.toString());
        String aiResponse = readAiResponse(connection);
        // 调用 AI 并获取指令
        String[] cmdLines = aiResponse.trim().split("\\n");
        List<String> successCmds = new ArrayList<>(); // 记录成功发送的指令，用于返回结果

        // 遍历每一行指令，解析主题和payload
        for (String cmdLine : cmdLines) {
            cmdLine = cmdLine.trim();
            // 跳过空行或格式错误的行（容错处理）
            if (StringUtils.isBlank(cmdLine) || !cmdLine.contains("|")) {
                continue;
            }

            // 按 "|" 拆分主题和指令（最多拆分为2部分，避免payload含"|"的异常）
            String[] topicAndPayload = cmdLine.split("\\|", 2);
            String topic = topicAndPayload[0].trim();
            String payload = topicAndPayload[1].trim();
            mqttPublishService.sendMessageAsync(topic, payload);
            successCmds.add(topic + ":" + payload); // 记录成功指令

        }
        return "已成功发送以下指令：\n" + String.join("\n", successCmds);
    }

    // ------------------------------ 工具方法：HTTP 请求与响应处理 ------------------------------
    /**
     * 创建 AI API 的 HTTP 连接，并写入请求体
     */
    private HttpURLConnection createAiHttpConnection(String reqBody) throws Exception {
        URL url = new URL(AI_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        // 设置请求头（UTF-8 编码、认证信息）
        connection.setRequestProperty("Content-Type", "application/json; charset=" + CHARSET);
        connection.setRequestProperty("Accept", "application/json; charset=" + CHARSET);
        connection.setRequestProperty("Authorization", "Bearer " + apiPassword);
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000); // 连接超时（5秒）
        connection.setReadTimeout(10000);   // 读取超时（10秒）

        // 写入请求体（try-with-resources 自动关闭流）
        try (OutputStream os = connection.getOutputStream()) {
            byte[] bodyBytes = reqBody.getBytes(StandardCharsets.UTF_8);
            os.write(bodyBytes);
            os.flush(); // 补充完成 flush 逻辑，确保请求体完整发送
        }
        return connection;
    }

    /**
     * 读取 AI API 的响应，提取 message.content 内容
     */
    private String readAiResponse(HttpURLConnection connection) throws Exception {
        // 先判断响应状态码，非 200 则抛出异常
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            // 读取错误响应内容
            String errorMsg = readErrorResponse(connection);
            throw new RuntimeException("AI API 请求失败，状态码：" + responseCode + "，错误信息：" + errorMsg);
        }

        // 读取成功响应（try-with-resources 自动关闭流）
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String inputLine;
            StringBuilder responseSb = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseSb.append(inputLine);
            }
            String fullResponse = responseSb.toString();
            System.out.println("AI API 原始响应：" + fullResponse);

            // 解析响应，提取 message.content（讯飞 API 响应格式）
            JSONObject respJson = new JSONObject(fullResponse);
            JSONArray choices = respJson.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("AI 响应中无 choices 数据");
            }
            JSONObject firstChoice = choices.getJSONObject(0);
            JSONObject message = firstChoice.getJSONObject("message");
            if (message == null || !message.containsKey("content")) {
                throw new RuntimeException("AI 响应中无 message.content 数据");
            }
            return message.getStr("content").trim(); // 返回 AI 核心响应内容
        } finally {
            connection.disconnect(); // 无论成功失败，最终关闭连接
        }
    }

    /**
     * 读取 HTTP 错误响应（如 401 未授权、500 服务器错误）
     */
    private String readErrorResponse(HttpURLConnection connection) throws Exception {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            StringBuilder errorSb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                errorSb.append(line);
            }
            return errorSb.toString();
        }
    }

    /**
     * 辅助方法：将设备状态码转为中文描述（如 1→开，0→关）
     */
    private String convertStatusDesc(String statusCode) {
        return switch (statusCode) {
            case "1" -> "开";
            case "0" -> "关";
            case "on" -> "开";
            case "off" -> "关";
            default -> "未知状态（" + statusCode + "）";
        };
    }

    private String parseDeviceStatusByTopic(String topic, MqttEntity entity) {
        JSONObject payload = entity.getPayload();
        String devName = getDeviceNameByTopic(topic); // 设备中文名称
        String updateTime = entity.getTimestampString(); // 更新时间

        // 按设备编码（topic）分支解析，确保每种设备有独立的格式化逻辑
        return switch (topic) {
            // 1. 卧室设备：bedroom {"l0":0,"l1":0,"l2":1,"fan":0,"tv":1}
            case "bedroom" -> String.format(
                    "%s（编码：%s）：\n" +
                            "  - 0号灯状态：%s\n" +
                            "  - 1号灯状态：%s\n" +
                            "  - 2号灯状态：%s\n" +
                            "  - 风扇状态：%s\n" +
                            "  - 电视状态：%s\n" +
                            "  更新时间：%s",
                    devName, topic,
                    convertLightFanStatus(payload.getInt("l0", -1)),
                    convertLightFanStatus(payload.getInt("l1", -1)),
                    convertLightFanStatus(payload.getInt("l2", -1)),
                    convertLightFanStatus(payload.getInt("fan", -1)),
                    convertTvStatus(payload.getInt("tv", -1)),
                    updateTime
            );

            // 2. 卫生间设备：toilet {"toilet_state":0,"light_state":0,"fan_state":1}
            case "toilet" -> String.format(
                    "%s（编码：%s）：\n" +
                            "  - 卫生间使用状态：%s\n" +
                            "  - 灯光状态：%s\n" +
                            "  - 排风扇状态：%s\n" +
                            "  更新时间：%s",
                    devName, topic,
                    convertToiletState(payload.getInt("toilet_state", -1)),
                    convertLightFanStatus(payload.getInt("light_state", -1)),
                    convertLightFanStatus(payload.getInt("fan_state", -1)),
                    updateTime
            );

            // 3. 厨房设备：kitchen {"alarmbell":0,"gas":475,"fan_status":0}
            case "kitchen" -> String.format(
                    "%s（编码：%s）：\n" +
                            "  - 燃气报警器状态：%s\n" +
                            "  - 燃气浓度：%s ppm\n" +
                            "  - 厨房风扇状态：%s\n" +
                            "  更新时间：%s",
                    devName, topic,
                    convertAlarmStatus(payload.getInt("alarmbell", -1)),
                    payload.getInt("gas", -1) == -1 ? "未知" : payload.getInt("gas", -1),
                    convertLightFanStatus(payload.getInt("fan_status", -1)),
                    updateTime
            );

            // 4. 门禁系统：access_control {"isHuman":0,"isCall":0,"owner":1}
            case "access_control" -> String.format(
                    "%s（编码：%s）：\n" +
                            "  - 人体检测：%s\n" +
                            "  - 呼叫状态：%s\n" +
                            "  - 主人模式：%s\n" +
                            "  更新时间：%s",
                    devName, topic,
                    convertHumanDetectStatus(payload.getInt("isHuman", -1)),
                    convertCallStatus(payload.getInt("isCall", -1)),
                    convertOwnerModeStatus(payload.getInt("owner", -1)),
                    updateTime
            );

            // 5. 客厅设备：living_room {"fire_alarm":0,"fire_state":0,"temp":33.20,"hum":0.00}
            case "living_room" -> String.format(
                    "%s（编码：%s）：\n" +
                            "  - 火灾报警器状态：%s\n" +
                            "  - 火灾检测状态：%s\n" +
                            "  - 温度：%s ℃\n" +
                            "  - 湿度：%s %%\n" +
                            "  更新时间：%s",
                    devName, topic,
                    convertAlarmStatus(payload.getInt("fire_alarm", -1)),
                    convertFireState(payload.getInt("fire_state", -1)),
                    payload.getDouble("temp", -1.0) == -1.0 ? "未知" : String.format("%.2f", payload.getDouble("temp", -1.0)),
                    payload.getDouble("hum", -1.0) == -1.0 ? "未知" : String.format("%.2f", payload.getDouble("hum", -1.0)),
                    updateTime
            );

            // 6. 淋浴间设备：shower_room_status {"pump_level": 0, "status": "active"}
            case "shower_room_status" -> String.format(
                    "%s（编码：%s）：\n" +
                            "  - 运行状态：%s\n" +
                            "  - 水泵档位：%s\n" +
                            "  更新时间：%s",
                    devName, topic,
                    convertShowerStatus(payload.getStr("status", "unknown")),
                    payload.getInt("pump_level", -1) == -1 ? "未知" : payload.getInt("pump_level", -1),
                    updateTime
            );

            // 7. 大灯设备：led_1_status {"state":1, "level":75}
            case "led_1_status" -> String.format(
                    "%s（编码：%s）：\n" +
                            "  - 运行状态：%s\n" +
                            "  - 亮度：%s（1-100）\n" +
                            "  更新时间：%s",
                    devName, topic,
                    convertLightFanStatus(payload.getInt("state", -1)), // 复用灯光状态转换（0=关，1=开）
                    payload.getInt("level", -1) == -1 ? "未知" : payload.getInt("level", -1),
                    updateTime
            );

            // 8. 灌溉水泵设备：water_pump_1_status {"pump_level":2, "mode":"auto", "moisture":65}
            case "water_pump_1_status" -> String.format(
                    "%s（编码：%s）：\n" +
                            "  - 水泵强度：%s 档（1-3）\n" +
                            "  - 运行模式：%s\n" +
                            "  - 当前湿度：%s %%\n" +
                            "  更新时间：%s",
                    devName, topic,
                    payload.getInt("pump_level", -1) == -1 ? "未知" : payload.getInt("pump_level", -1),
                    convertPumpMode(payload.getStr("mode", "unknown")), // 新增水泵模式转换
                    payload.getInt("moisture", -1) == -1 ? "未知" : payload.getInt("moisture", -1),
                    updateTime
            );

            // 未知设备（防新增设备未适配的情况）
            default -> String.format(
                    "未知设备（编码：%s）：\n" +
                            "  原始数据：%s\n" +
                            "  更新时间：%s",
                    topic, payload.toString(), updateTime
            );
        };
    }

    // 新增：水泵模式状态转换（补充配套方法）
    private String convertPumpMode(String mode) {
        return switch (mode.trim().toLowerCase()) {
            case "auto" -> "自动模式";
            case "manual" -> "手动模式";
            default -> "未知模式（" + mode + "）";
        };
    }


    // ------------------------------ 辅助方法：设备名称/状态转换（一一对应你的数据） ------------------------------
    /**
     * 按topic获取设备中文名称
     */
    private String getDeviceNameByTopic(String topic) {
        return switch (topic) {
            case "bedroom" -> "卧室";
            case "toilet" -> "卫生间";
            case "kitchen" -> "厨房";
            case "access_control" -> "门禁系统";
            case "living_room" -> "客厅";
            case "shower_room_status" -> "淋浴间";
            case "led_1_status" -> "大灯";
            case "water_pump_1_status" -> "水泵";
            default -> "未知设备";
        };
    }

    /**
     * 灯光/风扇状态转换（0=关，1=开）
     */
    private String convertLightFanStatus(int status) {
        return switch (status) {
            case 0 -> "关";
            case 1 -> "开";
            default -> "未知";
        };
    }

    /**
     * 电视状态转换（0=关，1=开）
     */
    private String convertTvStatus(int status) {
        return switch (status) {
            case 0 -> "关";
            case 1 -> "开";
            default -> "未知";
        };
    }

    /**
     * 卫生间使用状态转换（0=空闲，1=占用）
     */
    private String convertToiletState(int state) {
        return switch (state) {
            case 0 -> "空闲";
            case 1 -> "占用";
            default -> "未知";
        };
    }

    /**
     * 报警器状态转换（0=正常，1=报警）
     */
    private String convertAlarmStatus(int status) {
        return switch (status) {
            case 0 -> "正常（无报警）";
            case 1 -> "报警（异常）";
            default -> "未知";
        };
    }

    private String convertLedStatus(int status){
        return switch (status){
            case 0 -> "关闭状态";
            case 1 -> "正常运行";
            default -> "未知";
        };
    }

    /**
     * 人体检测状态转换（0=无人，1=有人）
     */
    private String convertHumanDetectStatus(int status) {
        return switch (status) {
            case 0 -> "无人";
            case 1 -> "有人";
            default -> "未知";
        };
    }

    /**
     * 呼叫状态转换（0=无呼叫，1=有呼叫）
     */
    private String convertCallStatus(int status) {
        return switch (status) {
            case 0 -> "无呼叫";
            case 1 -> "有呼叫";
            default -> "未知";
        };
    }

    /**
     * 主人模式状态转换（0=关闭，1=开启）
     */
    private String convertOwnerModeStatus(int status) {
        return switch (status) {
            case 0 -> "关闭（访客模式）";
            case 1 -> "开启（主人模式）";
            default -> "未知";
        };
    }

    /**
     * 火灾检测状态转换（0=无火灾，1=有火灾）
     */
    private String convertFireState(int state) {
        return switch (state) {
            case 0 -> "无火灾（正常）";
            case 1 -> "有火灾（紧急）";
            default -> "未知";
        };
    }

    /**
     * 淋浴间运行状态转换（active=运行中，inactive=未运行）
     */
    private String convertShowerStatus(String status) {
        return switch (status.trim().toLowerCase()) {
            case "active" -> "运行中";
            case "inactive" -> "未运行";
            default -> "未知（" + status + "）";
        };
    }



    private String getFieldDisplayName(String deviceCode, String field) {
        switch (deviceCode) {
            case "bedroom":
                return switch (field) {
                    case "l0" -> "0号灯状态";
                    case "l1" -> "1号灯状态";
                    case "l2" -> "2号灯状态";
                    case "fan" -> "风扇状态";
                    case "tv" -> "电视状态";
                    default -> field;
                };
            case "toilet":
                return switch (field) {
                    case "toilet_state" -> "使用状态";
                    case "light_state" -> "灯光状态";
                    case "fan_state" -> "排风扇状态";
                    default -> field;
                };
            // 其他设备的字段中文名称映射（厨房、客厅等）
            case "kitchen":
                return switch (field) {
                    case "alarmbell" -> "燃气报警器状态";
                    case "gas" -> "燃气浓度";
                    case "fan_status" -> "厨房风扇状态";
                    default -> field;
                };
            case "living_room":
                return switch (field) {
                    case "temp" -> "温度";
                    case "hum" -> "湿度";
                    case "fire_alarm" -> "火灾报警器状态";
                    case "fire_state" -> "火灾检测状态";
                    default -> field;
                };
                case "shower_room_status":
                    return switch (field){
                        case "pump_level" -> "水泵等级";
                        case "status" -> "状态";
                        default -> field;
                    };
                    case "led_1_status":
                        return switch (field){
                            case "state" -> "状态";
                            case "level" ->"亮度";
                            default -> field;
                        };
                        case "water_pump_1_status":
                            return switch (field){
                                case "pump_level" -> "水泵强度";
                                case "mode" -> "水泵模式";
                                case "moisture" -> "湿度";
                                default -> field;
                            };

            // 省略其他设备的映射...
            default:
                return field;
        }
    }
}