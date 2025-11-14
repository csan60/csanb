

local Sensors ={51,50,45,40,35,30,25,20,5,0,-10,-20,-21,-22}

local pre_time,cur_time,index=0,0,0


function recever_fen(ch,res)
  cur_time =etimer.now()
  print("cur_time-pre_time:",cur_time-pre_time)
  pre_time =cur_time
      
end


function entry()
  -- 此处输入程序代码
  on_buff_recv(channels.Fan,recever_fen)

  for _, v in ipairs(Sensors) do
    write_msg(channels.Sensor,protocols.P_Sensor,{temprature =v})     
    etimer.delay(100)
  end
  etimer.delay(10000)
  exit()
end