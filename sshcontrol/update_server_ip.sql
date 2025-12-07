-- Update server IP from 10.10.14.230 to 192.168.1.234
UPDATE servers SET ip = '192.168.1.234' WHERE name = 'Test 1';

-- Verify the update
SELECT id, name, ip, ssh_username FROM servers WHERE name = 'Test 1';
