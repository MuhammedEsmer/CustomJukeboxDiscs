#!/usr/bin/env bash
set -euo pipefail

readonly source_dir="/mnt/c/Users/Muhammed/Projects/CustomJukeboxDiscs/youtube-service"
readonly target_dir="/opt/customjukeboxdiscs-youtube"
readonly server_config="/srv/hoodoo-panel-lab/servers/hoodoo-minimal-1211/config/customjukeboxdiscs-youtube-server.toml"
readonly config_backup="${server_config}.before-service-setup-20260913"
readonly token_file="/tmp/cjd-youtube-token"

install -d -m 0755 "$target_dir"
cp -a "$source_dir/." "$target_dir/"

if [[ ! -f "$target_dir/.env" ]]; then
    cp "$target_dir/.env.example" "$target_dir/.env"
fi
sed -i 's/\r$//' "$target_dir/.env"

service_token="$(sed -n 's/^CJD_SERVICE_TOKEN=//p' "$target_dir/.env")"
if [[ -z "$service_token" || "$service_token" == "replace-with-a-long-random-value" ]]; then
    openssl rand -out "$token_file" -hex 32
    IFS= read -r service_token < "$token_file"
    sed -i "s|^CJD_SERVICE_TOKEN=.*|CJD_SERVICE_TOKEN=$service_token|" "$target_dir/.env"
fi
sed -i 's|^RAPIDAPI_KEY=replace-with-your-rapidapi-key$|RAPIDAPI_KEY=BURAYA_RAPIDAPI_KEY|; s|^RAPIDAPI_USERNAME=replace-with-your-rapidapi-username$|RAPIDAPI_USERNAME=BURAYA_RAPIDAPI_USERNAME|' "$target_dir/.env"
chmod 600 "$target_dir/.env"

install -m 0644 "$target_dir/deploy/customjukeboxdiscs-youtube.service" /etc/systemd/system/customjukeboxdiscs-youtube.service
sed -i 's/\r$//' /etc/systemd/system/customjukeboxdiscs-youtube.service

if [[ ! -f "$config_backup" ]]; then
    cp -p "$server_config" "$config_backup"
fi
sed -i "s|^[[:space:]]*token = .*|\ttoken = \"$service_token\"|" "$server_config"

rm -f "$token_file"
systemctl daemon-reload
systemctl enable customjukeboxdiscs-youtube.service

echo "Service files installed."
echo "RapidAPI credentials are the only remaining setup."
