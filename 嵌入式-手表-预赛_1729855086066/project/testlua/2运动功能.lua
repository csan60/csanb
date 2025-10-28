-- lua程序入口函数
function entry()
	-- 此处输入程序代码

	-- for i=1,500 do
	--   local x=math.random(-100,100)
	-- 	local y=math.random(-100,100)
	-- 	local z=math.random(-100,100)
	-- 	write_msg(channels.senser, protocols.P_acc,{acc_x= x, acc_y= y,acc_z= z})
	-- 	etimer.delay(50)
	-- end

  for j=1,100 do
	  for i=1,#test_data.speedInfo do
	    local x = test_data.speedInfo[i].acc_x
	    local y = test_data.speedInfo[i].acc_y
	    local z = test_data.speedInfo[i].acc_z
	    write_msg(channels.senser, protocols.P_acc, {acc_x=x, acc_y=y,acc_z=z})
		etimer.delay(100)
	  end
	  etimer.delay(100)
	end

  clear(channels.senser)
  etimer.delay(1000)

  print("运动结束")

	exit()
end