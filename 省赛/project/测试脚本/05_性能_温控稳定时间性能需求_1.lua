-- 性能1

local time0
local time1
local time2
local V =0
local Tc = 10    --设定温度
local T0 = 0      --初始恒温箱外部温度值
local T = T0
local n = 0
local Fs = 0
local Va = 0   --上一次的输出电压
local Qo =0
local Qi =0
local Start =false

function recever_heater(ch,res)
  local msg =unpack(protocols.P_Heater,res.value,true)
  if msg and Start then
    V =msg.value.voltage
    if V <0 then
      v =0
    end
  end

end

function recever_fen(ch,res)
  local msg =unpack(protocols.P_Fan,res.value,true)
  if msg and Start then
    if msg.value.cmd ==1 then
      Fs =2
    elseif msg.value.cmd == 0  then
      Fs =0
    end
  end


end

function entry()
  -- time0 = etimer.now()

  -- while true do
  --   clear(channels.Heater)
  --   clear(channels.Fan)
  --   write_msg(channels.Sensor,protocols.P_Sensor,{temprature =T})    
  --   etimer.delay(1000)
  --   if not Start then
  --     on_buff_recv(channels.Heater,recever_heater)
  --     on_buff_recv(channels.Fan,recever_fen)
  --   end
  --   Start =true
  --   Qi =V*V*0.2
  --   Qo = 0.1*(T - T0) + Fs
  --   T =T+(Qi-Qo)
  --   if math.abs(T -Tc) <1 then
  --     n=n+1
  --   else
  --     n=0
  --   end
  
  --   if n >=10 then
  --         time1 = etimer.now()
  --         print ('稳定时间为: ',(time1-time0)/1000," 秒")
  --         print("time1-time0 :",time1-time0 )
  --         if time1-time0 <=60000 then
  --           print '温控稳定时间性能测试通过'
  --           break
  --         end
  --   end
  --   -- print("n:",n)
  --   time2 = etimer.now()
  --   if time2-time0>60000 then
  --     print '温控稳定时间性能测试不通过'
  --     break      
  --   end
  -- end

  -- off_recv(channels.Fan)
  -- off_recv(channels.Heater)

  exit()
end

