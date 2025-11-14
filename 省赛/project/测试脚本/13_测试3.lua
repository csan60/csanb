function entry()
  local res =write_msg(channels.Sensor,protocols.P_Sensor,{temprature =11,datatype1=0x02})
  etimer.delay(100)
  passed = ask('yesno',  {title='提示', msg='观察界面采集温度是否为11', default=true})
  local res =step("验证数据类型1=0x02","不显示采集到数据，软件做丢包处理","显示到采集的数据",not passed)
  check(res,"数据类型1=0x02,测试通过!","数据类型1=0x02,测试不通过")
  exit()
end