function entry()
    -- 此处输入程序代码
    clear(channels.Sensor)
    for idx, value in ipairs(test_data.Frames) do
      local frm =value;
      local res =write_msg(channels.Sensor,protocols.P_Sensor,{temprature = frm.temprature})
      print("第"..idx .."次期望显示温度值",frm.expect)
      etimer.delay(1000)
    end
    exit()
end