
require("e_checker")

local res
local toStart=false
local index =0
--加热棒输出接口
--前提条件 界面显示采集温度不等于11

function entry()
  clear(channels.Fan)

  res =write_msg(channels.Sensor,protocols.P_Sensor,{temprature =8}) -- 不加热
  etimer.delay(1500)
  local res =read_buff(channels.Fan,0,1000)
  local r =unpack(protocols.P_Fan,res.value,true)
  local data =pack(protocols.P_Fan, r.value)
  print(r.value.cmd)
  if r  then
    index =index+1
    check(r.value.header == 0xFFFA, "加热棒输出包头字段正确", "加热棒输出包头字段错误")
    check(r.value.datatype1 == 0x02, "加热棒输出数据类型1字段正确", "加热棒输出数据类型1字段错误")
    check(r.value.datatype2 == 0x22, "加热棒输出数据类型2字段正确", "加热棒输出数据类型2字段错误")
    check(r.value.datalen == 0x01, "加热棒输出数据长度字段正确", "加热棒输出数据长度字段错误")
    local checkVal = e_checker.SUM_8 (ebuff.from_buff(data.value),3,32)
    check(r.value.check == checkVal, "加热棒输出校验字段正确", "加热棒输出校验字段错误")
    check(r.value.tail == 0x0F, "加热棒输出包尾字段正确", "加热棒输出包尾字段错误")

  end  
  etimer.delay(1000)
  res =write_msg(channels.Sensor,protocols.P_Sensor,{temprature =20}) -- 加热
  etimer.delay(1500)
  local res =read_buff(channels.Fan,0,1000)
  local r =unpack(protocols.P_Fan,res.value,true)
  local data =pack(protocols.P_Fan, r.value)
  print(r.value.cmd)
  if r  then
    index =index+1
    check(r.value.header == 0xFFFA, "加热棒输出包头字段正确", "加热棒输出包头字段错误")
    check(r.value.datatype1 == 0x02, "加热棒输出数据类型1字段正确", "加热棒输出数据类型1字段错误")
    check(r.value.datatype2 == 0x22, "加热棒输出数据类型2字段正确", "加热棒输出数据类型2字段错误")
    check(r.value.datalen == 0x01, "加热棒输出数据长度字段正确", "加热棒输出数据长度字段错误")
    local checkVal = e_checker.SUM_8 (ebuff.from_buff(data.value),3,32)
    check(r.value.check == checkVal, "加热棒输出校验字段正确", "加热棒输出校验字段错误")
    check(r.value.tail == 0x0F, "加热棒输出包尾字段正确", "加热棒输出包尾字段错误")

  end  

  exit()

end