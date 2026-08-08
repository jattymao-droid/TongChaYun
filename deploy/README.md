# ͨ���� �� ��Ѷ�Ʊ�������˵��

������**wj.xmls.vip**  
PostgreSQL��`127.0.0.1:35432` / ���� `mm5621528`  
Redis��`127.0.0.1:26739` / ���� `mm5621528`

## һ�����ش��

�ڿ�������Ŀ��Ŀ¼ִ�У�

```bash
cd TongChaYun/deploy
chmod +x package.sh
./package.sh
```

���`deploy/release/tongchayun-latest.tar.gz`

## ����������׼����������

1. �Ѱ�װ **Nginx**��**PostgreSQL**��**Redis**���˿�/��������һ�£�
2. ��װ **Java 17+**�������̵� �� Java ��Ŀ������ / OpenJDK 17��
3. �ڱ�������վ������վ�㣺`wj.xmls.vip`����Ŀ¼���飺`/www/wwwroot/wj.xmls.vip`
4. ���� SSL ֤�飨Let��s Encrypt��

## �����ϴ���һ������

```bash
# �ϴ�ѹ������վ��Ŀ¼��
cd /www/wwwroot/wj.xmls.vip
tar -xzf tongchayun-latest.tar.gz
chmod +x start.sh bin/*.sh
./start.sh
```

`start.sh` �����Σ�

1. ��� Java / psql  
2. �������ݿ� `tongchayun` ������ SQL  
3. �Ժ�̨��ʽ���� `ruoyi-admin.jar`��8080��

ֹͣ����

```bash
./bin/stop.sh
```

## �ġ����� Nginx

������ `nginx/wj.xmls.vip.conf` �ϲ�������վ�����ã�

- `/` �� `admin-ui`�������ˣ�
- `/h5/` �� `h5`�������ʾ�/��ѯ��
- `/prod-api/` �� `127.0.0.1:8080`
- `/profile/` �� �ϴ�Ŀ¼ `uploadPath`

��������� Nginx��

## �塢����

| ��; | ��ַ |
|------|------|
| ������̨ | https://wj.xmls.vip/ |
| H5 ����ҳ | https://wj.xmls.vip/h5/ |
| API | https://wj.xmls.vip/prod-api/ |

Ĭ���˺ţ�`admin` / `admin123`���������޸ģ�

## ������������

�༭ `env.sh`��

- `DB_*` / `REDIS_*`�����ݿ��뻺��
- `SERVER_PORT`����˶˿ڣ�Ĭ�� 8080��
- `INIT_DB=force`��ǿ����ղ��ؽ��⣨Σ�գ�
- `TOKEN_SECRET`��JWT ��Կ����������޸ģ�

## �ߡ�Ŀ¼�ṹ����ѹ��

```
wj.xmls.vip/
  start.sh
  env.sh
  app/ruoyi-admin.jar
  admin-ui/          # �����˾�̬��Դ
  h5/                # H5 ��̬��Դ
  sql/               # ��ʼ���ű�
  bin/               # init_db / start_app / stop
  nginx/
  uploadPath/        # ���к��Զ��������ϴ��ļ�
  logs/
```
