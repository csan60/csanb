local T ={7,8,9}

function entry()
  for _, value in ipairs(T) do
    local res =write_msg(channels.Sensor,protocols.P_Sensor,{temprature =value})
    etimer.delay(100)
  end
  exit()
end