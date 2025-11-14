
local index =0

function entry()
    clear(channels.Fan)
    -- 此处输入程序代码
    on_buff_recv(channels.Fan,function (ch,res)
      index =index +1
      local r =unpack(protocols.P_Fan,res.value,true)
      if r and index==4 then
        step("当连续检测到3次当前温度小于等于设定温度时,散热风扇停止转动!","","",r.value.cmd==test_data.expect)
        off_recv(channels.Fan)
        exit()
      end
    end)

    local res =write_msg_sync(channels.Sensor,protocols.P_Sensor,{temprature = test_data.temprature1})
    etimer.delay(500)
    local res =write_msg_sync(channels.Sensor,protocols.P_Sensor,{temprature = test_data.temprature2})
    etimer.delay(500)
    local res =write_msg_sync(channels.Sensor,protocols.P_Sensor,{temprature = test_data.temprature1})
    etimer.delay(500)
end