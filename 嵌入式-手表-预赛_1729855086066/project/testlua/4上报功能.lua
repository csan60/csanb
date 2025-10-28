local BASE_TIME = os.time({ year = 2000, month = 1, day = 1, hour = 0, min = 0, sec = 0 }) or 0

local function calc_current_seconds()
  local now = os.time()
  if not BASE_TIME or not now then
    return 0
  end
  local diff = now - BASE_TIME
  if diff < 0 then
    return 0
  end
  local limit = os.time({ year = 2100, month = 1, day = 1, hour = 0, min = 0, sec = 0 })
  if limit then
    local maxSeconds = math.max(limit - BASE_TIME, 0)
    if diff > maxSeconds then
      return maxSeconds
    end
  end
  return diff
end

local function send_basic_profile()
  local payload = {
    currentSecends = calc_current_seconds(),
    sex = 2,
    height = 178,
    weight = 72,
    age = 28,
  }
  print("🚀 下发基本信息设置，确保手表处于最新用户参数")
  write_msg(channels.upper, protocols.P_Info, payload)
  etimer.delay(300)
end

local STILL_SECOND = {
  { acc_x = 0, acc_y = 0, acc_z = 0 },
  { acc_x = 1, acc_y = -1, acc_z = 0 },
  { acc_x = -1, acc_y = 1, acc_z = 0 },
  { acc_x = 0, acc_y = 0, acc_z = 1 },
  { acc_x = 1, acc_y = 0, acc_z = -1 },
  { acc_x = 0, acc_y = 1, acc_z = 0 },
  { acc_x = -1, acc_y = 0, acc_z = 1 },
  { acc_x = 0, acc_y = -1, acc_z = -1 },
  { acc_x = 1, acc_y = 0, acc_z = 0 },
  { acc_x = 0, acc_y = 1, acc_z = 0 },
}

local ACTIVE_SECOND = {
  { acc_x = 55, acc_y = 30, acc_z = -24 },
  { acc_x = -60, acc_y = -32, acc_z = 26 },
  { acc_x = 62, acc_y = 35, acc_z = -28 },
  { acc_x = -68, acc_y = -38, acc_z = 30 },
  { acc_x = 70, acc_y = 42, acc_z = -32 },
  { acc_x = -75, acc_y = -45, acc_z = 34 },
  { acc_x = 78, acc_y = 46, acc_z = -35 },
  { acc_x = -82, acc_y = -48, acc_z = 37 },
  { acc_x = 80, acc_y = 44, acc_z = -33 },
  { acc_x = -76, acc_y = -42, acc_z = 31 },
}

local SPRINT_SECOND = {
  { acc_x = 110, acc_y = 90, acc_z = -85 },
  { acc_x = -120, acc_y = -92, acc_z = 88 },
  { acc_x = 125, acc_y = 95, acc_z = -92 },
  { acc_x = -130, acc_y = -98, acc_z = 94 },
  { acc_x = 135, acc_y = 100, acc_z = -96 },
  { acc_x = -140, acc_y = -102, acc_z = 98 },
  { acc_x = 142, acc_y = 105, acc_z = -100 },
  { acc_x = -146, acc_y = -108, acc_z = 102 },
  { acc_x = 148, acc_y = 110, acc_z = -104 },
  { acc_x = -150, acc_y = -112, acc_z = 106 },
}

local function play_second(samples)
  for _, acc in ipairs(samples) do
    write_msg(channels.senser, protocols.P_acc, acc)
    etimer.delay(100)
  end
end

local function inject_motion()
  print("🚀 开始注入运动数据，涵盖静止、跑步、冲刺阶段")
  for _ = 1, 10 do
    play_second(STILL_SECOND)
  end
  for _ = 1, 20 do
    play_second(ACTIVE_SECOND)
  end
  for _ = 1, 20 do
    play_second(SPRINT_SECOND)
  end
  print("✅ 运动数据注入完成")
end

local function provide_calorie_samples()
  local samples = {
    { level = 1, calorie = 145.35 },
    { level = 2, calorie = 168.42 },
    { level = 3, calorie = 198.76 },
  }

  local expectedTotal = 0
  for _, item in ipairs(samples) do
    expectedTotal = expectedTotal + item.calorie
    write_msg(channels.upper, protocols.P_calc, {
      level = item.level,
      calorie = item.calorie,
    })
    etimer.delay(250)
  end
  return expectedTotal
end

local function wait_for_report(attempts, timeout)
  attempts = attempts or 6
  timeout = timeout or 2000
  for _ = 1, attempts do
    local response = read_msg(channels.upper, protocols.P_report, timeout)
    if response and type(response.value) == "table" then
      return response.value
    end
  end
  return nil
end

local function manual_report_confirmation(expectedCalorie)
  local msg = string.format(
    "未自动捕获到上报帧，请手动确认：\n  是否已经接收到信息上报？\n  卡路里应累计约 %.2f kcal。若显示正确请选择‘是’。",
    expectedCalorie
  )
  local confirmed = ask("yesno", {
    title = "手动确认上报结果",
    msg = msg,
    default = true,
  })

  check(confirmed,
        "✅ 手动确认上报结果正确",
        "❌ 手动确认上报结果失败，请检查上报数据")
end

local function validate_report(report, expectedCalorie)
  local hasReport = report ~= nil
  check(hasReport, "✅ 收到上报数据", "❌ 未收到任何上报数据")
  if not hasReport then
    return
  end

  local duration = tonumber(report.working_time or report.duration)
  local calorie = tonumber(report.Calorie or report.calorie)

  if duration and duration > 0 then
    check(true,
          string.format("✅ 运动时长输出合理：%.0f 秒", duration),
          "")
  else
    check(false,
          "",
          string.format("❌ 运动时长输出异常，期望得到正值，实际 %s", tostring(report.working_time)))
  end

  if calorie then
    local tolerance = math.max(0.1 * expectedCalorie, 5)
    local diff = math.abs(calorie - expectedCalorie)
    if diff <= tolerance then
      check(true,
            string.format("✅ 卡路里累计正确：%.2f", calorie),
            "")
    else
      check(false,
            "",
            string.format("❌ 卡路里累计偏差过大，期望 %.2f±%.2f，实际 %.2f", expectedCalorie, tolerance, calorie))
    end
  else
    check(false,
          "",
          "❌ 无法解析卡路里字段")
  end
end

function entry()
  ask("ok", { msg = "请在手表上点击“开始运动”按钮，随后保持运动状态。" })

  clear(channels.upper)
  clear(channels.senser)
  etimer.delay(300)

  send_basic_profile()
  inject_motion()

  print("🚀 模拟上位机按分钟下发卡路里数据")
  local expectedCalorie = provide_calorie_samples()

  ask("ok", { msg = "请在被测件上点击“停止运动”按钮触发数据上报。" })

  local report = wait_for_report(8, 2000)
  if report then
    validate_report(report, expectedCalorie)
  else
    manual_report_confirmation(expectedCalorie)
  end

  clear(channels.upper)
  clear(channels.senser)
  etimer.delay(500)

  print("✅ 上报功能验证结束。")
  exit()
end
