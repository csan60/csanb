local function send_acc_frame(acc, delay)
  write_msg(channels.senser, protocols.P_acc, {
    acc_x = acc.acc_x,
    acc_y = acc.acc_y,
    acc_z = acc.acc_z,
  })
  etimer.delay(delay or 80)
end

local function replay_motion_sequences(sequences, repeatCount)
  if #sequences == 0 then
    return
  end

  repeatCount = repeatCount or 20
  for _ = 1, repeatCount do
    for _, acc in ipairs(sequences) do
      send_acc_frame(acc, 80)
    end
  end
end

local function send_boundary_cases()
  local boundarySamples = {
    { acc_x = -200, acc_y = 0, acc_z = 0 },
    { acc_x = 0, acc_y = 200, acc_z = 0 },
    { acc_x = 0, acc_y = 0, acc_z = -200 },
    { acc_x = 150, acc_y = -150, acc_z = 150 },
    { acc_x = 100, acc_y = 100, acc_z = 100 },
    { acc_x = -100, acc_y = -100, acc_z = -100 },
  }

  for _, acc in ipairs(boundarySamples) do
    send_acc_frame(acc, 60)
  end
end

local function send_invalid_frames()
  local invalidCases = {
    { desc = "包头错误", payload = { header = 0x0000, acc_x = 10, acc_y = 10, acc_z = 10 } },
    { desc = "数据长度错误", payload = { len = 0x01, acc_x = -10, acc_y = -10, acc_z = -10 } },
    { desc = "校验和错误", payload = { check_num = 0x00, acc_x = 30, acc_y = 30, acc_z = 30 } },
    { desc = "包尾错误", payload = { tail = 0x0000, acc_x = -30, acc_y = -30, acc_z = -30 } },
  }

  for _, case in ipairs(invalidCases) do
    print(string.format("🚧 发送异常加速度帧：%s", case.desc))
    write_msg(channels.senser, protocols.P_acc, case.payload)
    etimer.delay(120)
  end

  -- 包前冗余字段
  local valid = pack(protocols.P_acc, { acc_x = 5, acc_y = 5, acc_z = 5 })
  local noise = ebuff.from_bytes("AA55")
  local merged = ebuff.merge(noise, valid.value)
  print("🚧 发送带冗余字段的加速度帧")
  write_buff(channels.senser, merged)
  etimer.delay(120)
end

function entry()
  clear(channels.senser)
  etimer.delay(200)

  local sequences = test_data.speedInfo or {}
  if #sequences == 0 then
    print("⚠️ 未读取到运动数据，使用默认加速度样本")
    sequences = {
      { acc_x = 0, acc_y = 0, acc_z = 0 },
      { acc_x = 20, acc_y = 15, acc_z = 30 },
      { acc_x = -35, acc_y = -40, acc_z = 10 },
      { acc_x = 60, acc_y = -55, acc_z = 45 },
      { acc_x = 100, acc_y = 95, acc_z = -90 },
      { acc_x = -100, acc_y = -90, acc_z = 85 },
    }
  end

  print("🚀 开始回放运动序列数据")
  replay_motion_sequences(sequences, 25)

  print("🚀 注入边界条件数据")
  send_boundary_cases()

  print("🚀 注入异常帧用于接口容错测试")
  send_invalid_frames()

  clear(channels.senser)
  etimer.delay(500)

  exit()
end
