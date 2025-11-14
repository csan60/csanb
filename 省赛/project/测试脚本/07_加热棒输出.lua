local index = 0
local v=0     
local T = {2.81,50,9.1,5,1.6,} --给定一组输入温度参数
local Td = 10
local Result ={}

function Volt(j,arr)
  print("arr:",arr)
  local ek1,ek2
  local DP=0.05
  local DI=0.1
  local Dd=0.1

  local ek = Td-arr[j]
  if j==1 then
      ek1 = 0
      ek2 = 0
  elseif(j==2) then
    print("Td,j,arr[j-1]",Td,j,arr[j-1])
    ek1 = Td-arr[j-1]
    ek2 =0
  else
    ek1 = Td-arr[j-1]
    ek2 = Td-arr[j-2]
  end
  V = DP * (ek-ek1) + DI*ek + Dd*(ek-2*ek1+ek2)
  print("V:",V)
  return V
end

function entry()
    
    clear(channels.Heater)
    on_buff_recv(channels.Heater,function (ch,res)
      index =index +1
      local r =unpack(protocols.P_Heater,res.value,true)
      if r  then
        print("第"..index.."次期望输出电压值:" ..Result[index].. ",实际输出电压值:",r.value.voltage)
      end
      if index==5 then
        exit()
      end

    end)

   
    for i in ipairs(T) do
      print("温度值:",T[i]) 
      local res =write_msg_sync(channels.Sensor,protocols.P_Sensor,{temprature = T[i]})
        v = v+Volt(i,T)
        if v<0 then
          v=0
        end
        table.insert(Result,i,v)
        etimer.delay(100)
      end

end