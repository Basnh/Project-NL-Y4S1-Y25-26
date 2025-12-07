CREATE TABLE IF NOT EXISTS firewall_rules (
    id BIGSERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    rule_value VARCHAR(255) NOT NULL,
    protocol VARCHAR(20) NOT NULL,
    action VARCHAR(20) NOT NULL,
    zone VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT true,
    permanent BOOLEAN DEFAULT false,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    FOREIGN KEY (server_id) REFERENCES servers(id) ON DELETE CASCADE
);

CREATE INDEX idx_firewall_rules_server_id ON firewall_rules(server_id);
CREATE INDEX idx_firewall_rules_zone ON firewall_rules(zone);
CREATE INDEX idx_firewall_rules_action ON firewall_rules(action);
