-- Script để upgrade user thành admin
-- Thay 'username_here' bằng username của người dùng muốn upgrade

UPDATE users 
SET role = 'ADMIN' 
WHERE username = 'h8nhut';

-- Xem kết quả
SELECT id, username, email, role, is_active FROM users WHERE username = 'h8nhut';
