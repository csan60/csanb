
--温度传感器输入接口测试
--前提条件 界面显示采集温度不等于11
function entry()
    
  --包头验证
  local res =write_msg(channels.Sensor,protocols.P_Sensor,{temprature =11,header =0xFAFF})
  etimer.delay(100)
  passed = ask('yesno',  {title='提示', msg='观察界面采集温度是否为11', default=true})
  local res =step("验证包头,包头=0xFAFF","采集温度不等于11","采集温度等于11",not passed)
  check(res,"界面采集温度不等于11,测试通过!","界面采集温度等于11,测试不通过")

  --数据类型1验证
  local res =write_msg(channels.Sensor,protocols.P_Sensor,{temprature =12,datatype1 =02})
  etimer.delay(100)
  passed = ask('yesno',  {title='提示', msg='观察界面采集温度是否为12', default=true})
  local res =step("验证数据类型1,数据类型1=0x02","采集温度不等于12","采集温度等于12",not passed)
  check(res,"界面采集温度不等于12,测试通过!","界面采集温度等于12,测试不通过")

  --数据类型2验证
  local res =write_msg(channels.Sensor,protocols.P_Sensor,{temprature =13,datatype2 =0x11})
  etimer.delay(100)
  passed = ask('yesno',  {title='提示', msg='观察界面采集温度是否为13', default=true})
  local res =step("验证数据类型2,数据类型2=0x11","采集温度不等于11","采集温度等于13",not passed)
  check(res,"界面采集温度不等于13,测试通过!","界面采集温度等于131,测试不通过")


  exit()
end