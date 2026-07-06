UPDATE users
SET password = '$2a$10$TfVdkoWPDnLNyJYaYmOR2u9Ewx3U.sgI9dbP3gnIRBiTkJ2Goox1O',
    updated_at = CURRENT_TIMESTAMP
WHERE student_number IN ('admin', 's1001', 's1002', 's1003', 's1004');
