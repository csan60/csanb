local function calc_current_seconds()
  local base = os.time({ year = 2000, month = 1, day = 1, hour = 0, min = 0, sec = 0 })
  local now = os.time()
  if not base or not now then
    return 0
  end
  local diff = now - base
  if diff < 0 then
    return 0
  end
  local maxSeconds = 0xFFFFFFFFFFFF
  if diff > maxSeconds then
    diff = maxSeconds
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
  write_msg(channels.upper, protocols.P_Info, payload)
  etimer.delay(200)
end

local function replay_motion(durationMs)
  local pattern = {
    { acc_x = 15, acc_y = 12, acc_z = 9 },
    { acc_x = -18, acc_y = 20, acc_z = -12 },
    { acc_x = 65, acc_y = -55, acc_z = 45 },
    { acc_x = -40, acc_y = -42, acc_z = 30 },
    { acc_x = 95, acc_y = 90, acc_z = -85 },
    { acc_x = -95, acc_y = -88, acc_z = 82 },
  }

  local base = #pattern * 80
  local slice = math.floor((durationMs or 0) / base)
  if slice < 1 then
    slice = 1
  end

  for _ = 1, slice do
    for _, acc in ipairs(pattern) do
      write_msg(channels.senser, protocols.P_acc, acc)
      etimer.delay(80)
    end
  end
end

local function provide_calorie_samples(samples)
  local expectedTotal = 0
  for _, item in ipairs(samples) do
    expectedTotal = expectedTotal + item.calorie
    write_msg(channels.upper, protocols.P_calc, {
      level = item.level,
      calorie = item.calorie,
    })
    etimer.delay(180)
  end
  return expectedTotal
end

local function wait_for_report(attempts, timeout)
  attempts = attempts or 5
  timeout = timeout or 2000
  for _ = 1, attempts do
    local response = read_msg(channels.upper, protocols.P_report, timeout)
    if response and type(response.value) == "table" then
      return response.value
    end
  end
  return nil
end

local function validate_report(report, expectedCalorie)
  local hasReport = report ~= nil
  check(hasReport, "✅ 收到上报数据", "❌ 未收到任何上报数据")
  if not hasReport then
    return
  end

  local duration = tonumber(report.working_time or report.duration)
  local calorie = tonumber(report.Calorie or report.calorie)

  check(duration ~= nil and duration > 0,
        string.format("✅ 运动时长输出合理：%.0f 秒", duration or -1),
        string.format("❌ 运动时长输出异常，期望得到正值，实际 %s", tostring(report.working_time)))

  local tolerance = math.max(0.1 * expectedCalorie, 5)
  check(calorie ~= nil and math.abs(calorie - expectedCalorie) <= tolerance,
        string.format("✅ 卡路里累计正确：%.2f", calorie or -1),
        string.format("❌ 卡路里累计偏差过大，期望 %.2f±%.2f，实际 %s", expectedCalorie, tolerance, tostring(report.Calorie)))
end

function entry()
  clear(channels.upper)
  clear(channels.senser)
  etimer.delay(300)

  send_basic_profile()
  replay_motion(6000)

  local calorieSamples = {
    { level = 1, calorie = 145.35 },
    { level = 2, calorie = 168.42 },
    { level = 3, calorie = 198.76 },
  }

  local expectedCalorie = provide_calorie_samples(calorieSamples)

  etimer.delay(1000)
  local report = wait_for_report(6, 1500)
  validate_report(report, expectedCalorie)

  clear(channels.upper)
  clear(channels.senser)
  etimer.delay(500)

  exit()
end
