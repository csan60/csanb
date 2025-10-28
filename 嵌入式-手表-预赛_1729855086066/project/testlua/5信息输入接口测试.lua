-- lua程序入口函数
function entry()
  -- 此处输入程序代码


  -- 包头错误的数据包
  write_msg(channels.upper, protocols.P_Info,{header=0xFFFF,age=1})
  etimer.delay(2000)
  local res = ask("yesno",{title='提示', msg='包头错误的数据包，请确认基本信息是否设置成功', default=false})
	check(not res, "包头错误,有丢包处理，正确", "包头错误，没有丢包处理，错误")

  -- 数据长度错误的数据包
  write_msg(channels.upper, protocols.P_Info,{len=0xF,age=2})
  etimer.delay(2000)
  local res = ask("yesno",{title='提示', msg='数据长度错误的数据包，请确认基本信息是否设置成功', default=false})
  check(not res, "数据长度错误,有丢包处理，正确", "数据长度错误，没有丢包处理，错误")

  -- 校验和错误的数据包
  write_msg(channels.upper, protocols.P_Info,{check_code=0xFFFF,age=3})
  etimer.delay(2000)
  local res = ask("yesno",{title='提示', msg='校验和错误的数据包，请确认基本信息是否设置成功', default=false})
  check(not res, "校验和错误,有丢包处理，正确", "校验和错误，没有丢包处理，错误")

  -- 包尾错误的数据包
  write_msg(channels.upper, protocols.P_Info,{tail=0xFFFF,age=4})
  etimer.delay(2000)
  local res = ask("yesno",{title='提示', msg='包尾错误的数据包，请确认基本信息是否设置成功', default=false})
  check(not res, "包尾错误,有丢包处理，正确", "包尾错误，没有丢包处理，错误")


  -- 包前有冗余字段
  local res = pack(protocols.P_Info,{age=8})
  local buff1 = ebuff.from_bytes("AA")
  local buff = ebuff.merge(buff1,res.value)
  write_buff(channels.upper,buff)
  etimer.delay(2000)
  local res2 = ask("yesno",{title='提示', msg='包前有冗余字段，请确认基本信息是否设置成功', default=false})
  check(res2, "包前有冗余字段,没有丢包处理，正确", "包前有冗余字段,有丢包处理，错误")

  clear(channels.upper)
  etimer.delay(1000)

  exit()
end