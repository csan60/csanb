function entry()
  print("=== 🚀 开始测试智能手表上报功能 ===")

  ask("ok", { msg = "请点击『停止运动』按钮" })

  -- 🧩 调试阶段：打印 read_msg 返回情况
  print("🧩 等待上报帧 ...")
  local res = read_msg(channels.upper, protocols.P_report, 5000)
  print("🧩 read_msg 返回类型:", type(res))
  print("🧩 read_msg 返回内容:", tostring(res))

  -- 🧩 Step 1: 判空
  if res == nil then
      print("❌ 未接收到上报帧（res 为 nil，可能超时或协议号不对）")
      exit()
      return
  end

  -- 🧩 Step 2: 类型校验
  if type(res) ~= "table" then
      print("⚠️ 返回值不是表，实际类型为:", type(res))
      exit()
      return
  end

  -- 🧩 Step 3: 安全检查 res.value
  local value = res.value
  if not value or type(value) ~= "table" then
      print("⚠️ res.value 不存在或不是表！")
      print("📦 原始内容:", tostring(res))
      exit()
      return
  end

  -- 🧩 Step 4: 提取字段
  local duration = tonumber(value.duration) or 0
  local calorie = tonumber(value.calorie) or 0

  print(string.format("✅ 运动时长: %d 秒", duration))
  print(string.format("✅ 卡路里: %.2f kcal", calorie))

  if duration == 0 and calorie == 0 then
      print("⚠️ 数据为 0，可能未正确上报")
  else
      print("🎯 上报功能正常")
  end

  print("=== ✅ 测试结束 ===")
  exit()
end
