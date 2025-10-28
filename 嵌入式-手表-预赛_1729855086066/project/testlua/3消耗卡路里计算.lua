-- lua程序入口函数
local sex ,age,height,weight=0,0,0,0
local calorie =0.0;
local i = 0

local tbl_male = 1.55
function entry()
	-- 此处输入程序代码

  --不同级别消耗的卡路里数据

  for _, v in ipairs(test_data.info) do
    sex =v.gender
    age =v.age
    height =v.height
    weight = v.weight
    calorie=(66+13.7*weight+5*height-6.8*age)*tbl_male

    write_msg(channels.upper, protocols.P_calc, {level = i,calorie =calorie})
    print("level:sex:age:height:weight:calorie:",i-1,sex,age,height,weight,calorie)

    etimer.delay(1000)
  end

  
  clear(channels.upper)
  etimer.delay(1000)

	exit()
end