# Simple API application

# 🚀 Пошаговый гайд: деплой Spring Boot приложения в VirtualBox/Ubuntu через NAT и Nginx (с учетом реальных ошибок)

> Настроено для демонстрации с Windows-хоста через NAT-проброс портов, c корректной работой nginx и firewall.  
> Все шаги протестированы, с примерами команд и решением типовых проблем.

---

## 1. Подготовка VirtualBox VM

- Установите Ubuntu (рекомендуется 22.04 LTS).
- В VirtualBox Manager → VM Settings → **Network**:
  - Adapter 1: **NAT** (обязательно! — для Port Forwarding)
  - Adapter 2: Host-Only (по желанию, для прямого доступа по внутренней сети)

---

## 2. Установка необходимых пакетов

```bash
sudo apt update && sudo apt -y upgrade
sudo apt -y install openjdk-21-jdk maven git nginx curl tcpdump
````

---

## 3. Клонирование и сборка приложения
```bash
mkdir -p ~/app && cd ~/app
git clone https://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@github.com/AaronSigel/simple-api
cd simple-api
mvn clean package -DskipTests
```

> Для приватных репозиториев используйте Personal Access Token.

---

## 4. Копирование JAR в папку деплоя

```bash
sudo mkdir -p /var/www/app
sudo cp target/*.jar /var/www/app/app.jar
sudo chown www-data:www-data /var/www/app/app.jar
```

---

## 5. Создание systemd-сервиса

Создайте файл `/etc/systemd/system/simple-api.service` со следующим содержимым:

```ini
[Unit]
Description=Simple Spring Boot API
After=network.target

[Service]
User=www-data
WorkingDirectory=/var/www/app
ExecStart=/usr/bin/java -jar /var/www/app/app.jar --server.port=5000
SuccessExitStatus=143
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

Затем выполните:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now simple-api
sudo systemctl status simple-api --no-pager
```

---

## 6. Настройка nginx как реверс-прокси (80 → 5000)

Создайте файл `/etc/nginx/sites-available/simple-api`:

```nginx
server {
    listen 80 default_server;
    server_name _;

    location / {
        proxy_pass http://127.0.0.1:5000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Включите сайт и отключите дефолтный:

```bash
sudo ln -sf /etc/nginx/sites-available/simple-api /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

---

## 7. Проверка приложения внутри VM

```bash
curl -v http://127.0.0.1/health
```

Ожидается: HTTP/1.1 200 OK и ответ "OK".

Если не работает, проверьте логи:

```bash
sudo journalctl -u simple-api -f
sudo tail -f /var/log/nginx/access.log
```

---

## 8. Проверка, что nginx слушает нужный интерфейс

```bash
sudo ss -ltnp | grep ':80'
```

Должно быть: `LISTEN 0.0.0.0:80` или `*:80` (не только 127.0.0.1).

---

## 9. Открытие порта 80 в UFW и/или iptables

```bash
sudo ufw allow 80/tcp
sudo ufw allow in on enp0s3 to any port 80 proto tcp comment 'HTTP NAT'
sudo ufw status
```

Для iptables (если используется):

```bash
sudo iptables -I INPUT -p tcp --dport 80 -j ACCEPT
```

---

## 10. Настройка Port Forwarding в VirtualBox

1. Выключите VM.
2. VirtualBox → Settings → Network → Adapter 1 (NAT) → **Port Forwarding**.
3. Добавьте правило:

| Name | Protocol | Host IP | Host Port | Guest IP | Guest Port |
| ---- | -------- | ------- | --------- | -------- | ---------- |
| http | TCP      | (пусто) | 8080      | (пусто)  | 80         |

4. Сохраните и **перезапустите VM**.

---

## 11. Проверка доступа из Windows

```powershell
curl -v http://127.0.0.1:8080/health
# или
Invoke-WebRequest http://127.0.0.1:8080/health
```

Ожидается: HTTP/1.1 200 OK, ответ "OK".

Если "висит", проверьте, не занят ли порт 8080 (`netstat -ano | findstr :8080`), а также убедитесь, что правило Port Forwarding настроено для **NAT**-адаптера.

---

## 12. Диагностика (типовые ошибки)

| Ошибка                  | Причина и решение                                                                               |
| ----------------------- | ----------------------------------------------------------------------------------------------- |
| 404 от nginx            | Активен дефолтный сайт. Удалите `/etc/nginx/sites-enabled/default`, reload nginx.               |
| "Висит" запрос          | Nginx слушает только 127.0.0.1. Исправьте на `listen 80 default_server;` и reload nginx.        |
| SYN/ACK не возвращается | Nginx не слушает внешний интерфейс, либо firewall блокирует порт 80. Проверьте ss/ufw/iptables. |
| Нет пакетов в tcpdump   | Port Forwarding не сработал, перезапустите VM, измените Host IP правила на пусто ("").          |

---

## 13. Полезные команды для отладки

```bash
sudo tail -f /var/log/nginx/access.log
sudo journalctl -u simple-api -f
sudo ss -ltnp
sudo tcpdump -nn -i enp0s3 tcp port 80
curl http://127.0.0.1:5000/health
curl http://10.0.2.15/health
```

---

## 14. Итоговая проверка

```powershell
curl -v http://127.0.0.1:8080/health
```

Ожидается: HTTP/1.1 200 OK и тело "OK".

---

## 15. Советы

* Для демонстрации через Windows используйте NAT + Port Forwarding (настроен на шаге 10).
* Не забывайте reload nginx и перезапускать VM после изменения Port Forwarding.
* Все изменения firewall/iptables выполняйте осознанно.
* Для локальной сети (host-only) используйте отдельный сетевой интерфейс, при необходимости добавьте статический маршрут.
