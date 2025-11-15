
-- 接收靶区信息
function Target()

  write_msg(channels.COM_target,protocols.target,{zone=1})
  etimer.delay(1000)

end

function entry()
    -- 此处输入程序代码
    Target()
    exit()
end