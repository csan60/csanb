
require("e_checker")

--加热棒输出接口
--前提条件 界面显示采集温度不等于11

function entry()

  local res =write_msg(channels.Sensor,protocols.P_Sensor,{temprature =20}) -- 不加热
  res =read_buff(channels.Heater,0,1000)
	local data = res.value
	local pkg = unpack(protocols.P_Heater, ebuff.from_buff(data), true)
  if pkg then
    check(pkg.value.header == 0xFFFA, "加热棒输出包头字段正确", "加热棒输出包头字段错误")
    check(pkg.value.datatype1 == 0x02, "加热棒输出数据类型1字段正确", "加热棒输出数据类型1字段错误")
    check(pkg.value.datatype2 == 0x11, "加热棒输出数据类型2字段正确", "加热棒输出数据类型2字段错误")
    check(pkg.value.datalen == 0x4, "加热棒输出数据长度字段正确", "加热棒输出数据长度字段错误")
    local checkVal = e_checker.SUM_8 (ebuff.from_buff(data),3,56)
    check(pkg.value.check == checkVal, "加热棒输出校验字段正确", "加热棒输出校验字段错误")
    check(pkg.value.tail == 0x0F, "加热棒输出包尾字段正确", "加热棒输出包尾字段错误")
  end

  local res =write_msg(channels.Sensor,protocols.P_Sensor,{temprature =5}) -- 不加热
  res =read_buff(channels.Heater,0,1000)
	local data = res.value
	local pkg = unpack(protocols.P_Heater, ebuff.from_buff(data), true)
  if pkg then
    check(pkg.value.header == 0xFFFA, "加热棒输出包头字段正确", "加热棒输出包头字段错误")
    check(pkg.value.datatype1 == 0x02, "加热棒输出数据类型1字段正确", "加热棒输出数据类型1字段错误")
    check(pkg.value.datatype2 == 0x11, "加热棒输出数据类型2字段正确", "加热棒输出数据类型2字段错误")
    check(pkg.value.datalen == 0x4, "加热棒输出数据长度字段正确", "加热棒输出数据长度字段错误")
    local checkVal = e_checker.SUM_8 (ebuff.from_buff(data),3,56)
    check(pkg.value.check == checkVal, "加热棒输出校验字段正确", "加热棒输出校验字段错误")
    check(pkg.value.tail == 0x0F, "加热棒输出包尾字段正确", "加热棒输出包尾字段错误")
  end

  exit()

end