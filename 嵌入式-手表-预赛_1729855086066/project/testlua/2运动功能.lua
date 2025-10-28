local function send_acc_frame(acc, delayMs)
  write_msg(channels.senser, protocols.P_acc, {
    acc_x = acc.acc_x,
    acc_y = acc.acc_y,
    acc_z = acc.acc_z,
  })
  etimer.delay(delayMs or 100)
end

local function play_second(samples)
  for _, acc in ipairs(samples) do
    send_acc_frame(acc, 100)
  end
end

local function play_segment(label, samples, seconds)
  print(string.format("🚀 发送运动片段《%s》，持续 %d 秒", label, seconds))
  for _ = 1, seconds do
    play_second(samples)
  end
end

local STILL_PROFILE = {
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

local WALK_PROFILE = {
  { acc_x = 10, acc_y = 5, acc_z = 3 },
  { acc_x = -8, acc_y = -4, acc_z = 2 },
  { acc_x = 12, acc_y = 6, acc_z = -3 },
  { acc_x = -15, acc_y = -7, acc_z = 4 },
  { acc_x = 18, acc_y = 8, acc_z = -5 },
  { acc_x = -20, acc_y = -10, acc_z = 6 },
  { acc_x = 14, acc_y = 9, acc_z = -7 },
  { acc_x = -12, acc_y = -8, acc_z = 5 },
  { acc_x = 10, acc_y = 6, acc_z = -4 },
  { acc_x = -8, acc_y = -5, acc_z = 3 },
}

local RUN_PROFILE = {
  { acc_x = 40, acc_y = 25, acc_z = -20 },
  { acc_x = -45, acc_y = -30, acc_z = 18 },
  { acc_x = 55, acc_y = 32, acc_z = -22 },
  { acc_x = -60, acc_y = -35, acc_z = 24 },
  { acc_x = 65, acc_y = 40, acc_z = -26 },
  { acc_x = -70, acc_y = -42, acc_z = 28 },
  { acc_x = 60, acc_y = 38, acc_z = -24 },
  { acc_x = -58, acc_y = -36, acc_z = 20 },
  { acc_x = 52, acc_y = 30, acc_z = -18 },
  { acc_x = -50, acc_y = -28, acc_z = 16 },
}

local SPRINT_PROFILE = {
  { acc_x = 110, acc_y = 90, acc_z = -85 },
  { acc_x = -115, acc_y = -92, acc_z = 88 },
  { acc_x = 120, acc_y = 95, acc_z = -90 },
  { acc_x = -125, acc_y = -98, acc_z = 92 },
  { acc_x = 130, acc_y = 100, acc_z = -95 },
  { acc_x = -135, acc_y = -102, acc_z = 98 },
  { acc_x = 140, acc_y = 105, acc_z = -100 },
  { acc_x = -145, acc_y = -108, acc_z = 102 },
  { acc_x = 150, acc_y = 110, acc_z = -104 },
  { acc_x = -155, acc_y = -112, acc_z = 106 },
}

local function send_boundary_cases()
  print("🚧 发送边界强度样本，验证超范围截断")
  local boundarySamples = {
    { acc_x = -200, acc_y = 0, acc_z = 0 },
    { acc_x = 0, acc_y = 200, acc_z = 0 },
    { acc_x = 0, acc_y = 0, acc_z = -200 },
    { acc_x = 150, acc_y = -150, acc_z = 150 },
    { acc_x = 100, acc_y = 100, acc_z = 100 },
    { acc_x = -100, acc_y = -100, acc_z = -100 },
  }

  for _, acc in ipairs(boundarySamples) do
    send_acc_frame(acc, 80)
  end
end

local function send_invalid_frames()
  print("🚧 注入异常加速度帧，验证丢包处理")
  local invalidCases = {
    { desc = "包头错误", payload = { header = 0x0000, acc_x = 10, acc_y = 10, acc_z = 10 } },
    { desc = "数据长度错误", payload = { len = 0x01, acc_x = -10, acc_y = -10, acc_z = -10 } },
    { desc = "校验和错误", payload = { check_num = 0x00, acc_x = 30, acc_y = 30, acc_z = 30 } },
    { desc = "包尾错误", payload = { tail = 0x0000, acc_x = -30, acc_y = -30, acc_z = -30 } },
  }

  for _, case in ipairs(invalidCases) do
    print(string.format("    -> %s", case.desc))
    write_msg(channels.senser, protocols.P_acc, case.payload)
    etimer.delay(150)
  end

  local valid = pack(protocols.P_acc, { acc_x = 5, acc_y = 5, acc_z = 5 })
  local noise = ebuff.from_bytes("AA55")
  local merged = ebuff.merge(noise, valid.value)
  print("    -> 包前冗余字段")
  write_buff(channels.senser, merged)
  etimer.delay(150)
end

function entry()
  ask("ok", { msg = "请先在手表上点击“开始运动”按钮，进入运动监测状态。" })

  clear(channels.senser)
  etimer.delay(200)

  play_segment("静止对照", STILL_PROFILE, 10)
  play_segment("快走", WALK_PROFILE, 20)
  play_segment("跑步", RUN_PROFILE, 15)
  play_segment("冲刺", SPRINT_PROFILE, 15)

  send_boundary_cases()
  send_invalid_frames()

  clear(channels.senser)
  etimer.delay(500)

  print("✅ 运动数据注入完成，请保持手表处于运行状态，后续脚本将继续验证其它功能。")
  exit()
end
