-- lua程序入口函数
function entry()
	-- 此处输入程序代码

  --被测件的初始时间
	local timeStart = os.time({year=2000, month = 1, day =1, hour =0, min =0, sec = 0})
  --设置当前时间 
	local tmNow = os.time({year=2000, month = 1, day = 1, hour = 0, min = 0, sec =  0})

	local currentSecends = tmNow - timeStart
  if (currentSecends < 0) then 
    currentSecends = 0 
  end
  
  --性别：男 身高：180 体重：70kg 年龄：20
	local data = { sex=test_data.testdata.gender, height=test_data.testdata.height, weight=test_data.testdata.weight, age=test_data.testdata.age } 
  data["currentSecends"] = currentSecends
	write_msg(channels.upper, protocols.P_Info, data)
  etimer.delay(100)

  clear(channels.upper)
  etimer.delay(1000)

	exit()
end