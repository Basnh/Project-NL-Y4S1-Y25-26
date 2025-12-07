CREATE TABLE IF NOT EXISTS firewall_ip_rules (
    id BIGSERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL,
    ip_address VARCHAR(50) NOT NULL,
    action VARCHAR(20) NOT NULL,
    permanent BOOLEAN DEFAULT false,
    created_at BIGINT NOT NULL,
    FOREIGN KEY (server_id) REFERENCES servers(id) ON DELETE CASCADE
);

CREATE INDEX idx_firewall_ip_rules_server_id ON firewall_ip_rules(server_id);
CREATE INDEX idx_firewall_ip_rules_action ON firewall_ip_rules(action);
