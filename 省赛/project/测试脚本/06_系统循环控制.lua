local t1,t2=0,0

function recever_heater(ch,res)
  local msg =unpack(protocols.P_Heater,res.value,true)
  if msg then
    t2=etimer.now()
  end
end



function entry()
    -- 此处输入程序代码
    write_msg(channels.Sensor,protocols.P_Sensor,{temprature =2})     
    clear(channels.Heater)

  on_buff_recv(channels.Fan,recever_heater)

    for i = 1, 10, 1 do
      clear(channels.Heater)
      t1=etimer.now()
      print(t2-t1)    
    end

    exit()
end