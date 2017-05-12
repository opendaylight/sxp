-- based on trivial protocol example
-- declare our protocol - SXP4
sxp4_proto = Proto("sxp4", "Scalable-Group Tag eXchange Protocol - v4 Protocol")

require "sxp4-common"
require "sxp4-open-msg"
require "sxp4-update-msg"
require "sxp4-error-msg"

-- create a function to dissect it
function sxp4_proto.dissector(buffer, pinfo, tree)
    pinfo.cols.protocol = "SXP"
    local subtree = tree:add(sxp4_proto, buffer(), "SXP Data")
    local msg_length = buffer(0, 4)
    local msg_length_value = buffer(0, 4):uint()
    subtree:add(msg_length, "length: " .. msg_length:uint())

    local msg_type = buffer(4, 4)
    local msg_type_value = msg_type:uint()
    subtree:add(msg_type, "type: " .. SXP_MSG_TYPE[msg_type_value] .. " [" .. msg_type_value .. "]")

    local payloadBuffer = buffer(8, msg_length_value - SXP_HEADER_LENGTH)
    subtree = subtree:add(payloadBuffer, "payload: ")

    -- message body processing ----
    if msg_type_value == 1 or msg_type_value == 2 then
        -- OPEN / OPEN_RESP
        handle_open_msg(payloadBuffer, subtree)
    elseif msg_type_value == 3 then
        -- UPDATE
        handle_update_msg(payloadBuffer, subtree)
    elseif msg_type_value == 4 then
        -- ERROR
        handle_error_msg(payloadBuffer, subtree)
    elseif msg_type_value == 5 then
        -- PURGE_ALL
        subtree = subtree:add(payloadBuffer, "PURGE_ALL")
    elseif msg_type_value == 6 then
        -- KEEP_ALIVE
        subtree = subtree:add(payloadBuffer, "KEEP_ALIVE")
    end

    pinfo.cols["info"] = "SXP:" .. msg_type_value .. " -- " .. tostring(pinfo.cols["info"])

    return msg_length
end


-- REGISTRATION
-- load the tcp.port table
tcp_table = DissectorTable.get("tcp.port")
-- register our protocol to handle tcp port 64999
tcp_table:add(64999, sxp4_proto)
