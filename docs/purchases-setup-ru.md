# Настройка и ручная проверка покупок DriveCare Pro

Проверено по текущему коду и официальной документации 25 октября 2026.
Настройка выполняется в Google Play Console, Google Cloud и RevenueCat.
Firebase AI Logic и покупки настраиваются независимо.

## 1. Что именно продаёт приложение

В текущем коде реализован постоянный доступ DriveCare Pro за одну покупку.
Это не подписка и не расходуемые монеты. Идентификаторы должны совпасть точно:

| Где | Значение |
| --- | --- |
| Android package / applicationId | `com.example.drivecare` |
| Google Play product ID | `drivecare_pro_lifetime` |
| RevenueCat entitlement ID | `pro` |
| RevenueCat offering ID | `default` |
| Package внутри offering | тип Lifetime (`$rc_lifetime`) |
| SDK key, принимаемый приложением | публичный Android-ключ `goog_…` |

Имена, показываемые пользователю, можно переводить. Эти технические идентификаторы
менять без соответствующего изменения кода нельзя. Приложение специально выбирает
`offerings["default"].lifetime` и проверяет product ID. Указание другого offering как
current по умолчанию не подменяет здесь offering с ID `default`.

Pro открывает несколько активных автомобилей, расширенные сравнения и PDF-отчёты.
Он не увеличивает лимит ИИ. Доступ определяется только активным entitlement `pro`
в CustomerInfo RevenueCat. Локального переключателя «сделать Pro» нет.

## 2. Подготовить приложение в Google Play

Откройте [Google Play Console](https://play.google.com/console/) и создайте приложение
в своём developer account. Пакет загруженной сборки должен быть именно
`com.example.drivecare`. Если это имя уже занято, потребуется согласованная смена
пакета и регистраций Firebase/RevenueCat перед публикацией, а не случайное изменение
одного из трёх мест.

В Android Studio: **Build → Generate Signed App Bundle / APK → Android App Bundle**,
выберите модуль `app`. Используйте свой upload keystore или создайте его через мастер.
Храните keystore и пароли в надёжном месте вне репозитория. Не используйте debug
сертификат в качестве схемы production-подписи. Подключите Play App Signing.

Загрузите подписанный `.aab` в **Testing → Internal testing**, завершите требования
консоли и выпустите сборку в этом треке. Для каждой следующей загрузки увеличивайте
`versionCode` в `app/build.gradle.kts`; текущая исходная версия — 1. Приложение уже
добавляет Google Play Billing permission через зависимость SDK.

## 3. Создать товар в Google Play

В выбранном приложении откройте **Monetize with Play → Products → One-time products**
(в некоторых вариантах интерфейса раздел называется **In-app products**).
Создайте товар с ID `drivecare_pro_lifetime`, названием «DriveCare Pro» и описанием
постоянного доступа. Настройте цену, страны и доступный purchase option типа Buy,
затем активируйте товар/вариант покупки согласно требованиям консоли. Не выбирайте
аренду или подписку. Для новой модели one-time products проверьте совместимость
варианта с используемой Billing Library; используйте один простой вариант покупки.

Название и цену приложение получает из магазина. Наличие созданного, но неактивного
товара не означает доступность покупки. Сведения и цены распространяются не мгновенно.
[Настройка Google Play product в RevenueCat](https://www.revenuecat.com/docs/getting-started/entitlements/android-products).

## 4. Создать Google Play app в RevenueCat

В [RevenueCat Dashboard](https://app.revenuecat.com/) создайте проект, затем добавьте
приложение магазина **Google Play**, указав точный package name. Для реальной проверки
Play используйте это магазинное приложение и Android SDK key. Текущий DriveCare не
настроен на RevenueCat Test Store и ключи `test_…`.

## 5. Соединить RevenueCat с Google Play

Откройте в RevenueCat настройки своего Play Store app и пройдите официальный мастер
[Google Play service credentials](https://www.revenuecat.com/docs/service-credentials/creating-play-service-credentials).
Для ручного пути в Google Cloud включите требуемые мастером API, создайте service
account и JSON-ключ. В Google Play Console → **Users and permissions** пригласите
email service account, предоставьте доступ к нужному приложению и права из текущего
мастера RevenueCat. Затем загрузите JSON в RevenueCat → **Apps → Play Store app →
Service Account Credentials JSON**, сохраните и запустите проверку credentials.

Не давайте произвольные роли Owner ради обхода ошибок. Новые credentials могут
активироваться до 36 часов. Следуйте результатам проверки в dashboard.

Это два разных файла:

| Файл | Куда относится |
| --- | --- |
| `google-services.json` | Firebase-конфигурация Android, лежит в `app/` |
| Service account JSON с приватным ключом | Серверные credentials RevenueCat; в приложение и Git не попадает |

## 6. Связать продукт, entitlement и offering

В RevenueCat откройте **Product catalog**, добавьте/импортируйте Google Play product
`drivecare_pro_lifetime` и обязательно задайте ему тип **Non-consumable**. Если
оставить Consumable, SDK может потребить покупку и магазин разрешит купить её снова.
Используемая версия SDK поддерживает non-consumable. Затем создайте entitlement
с ID `pro` и прикрепите к нему этот product.

Создайте offering с ID **`default`**. В нём добавьте package типа **Lifetime** и
выберите ваш Google Play product. Проверьте полную цепочку:

```text
Google Play: drivecare_pro_lifetime
       ↓
RevenueCat product: Non-consumable
       ↓
Entitlement: pro
       ↓
Offering: default → Lifetime package
```

Сохраните изменения. [Поддержка постоянных покупок](https://www.revenuecat.com/docs/platform-resources/non-subscriptions).

## 7. Добавить публичный SDK key в локальную сборку

В RevenueCat → **Project settings → API keys** найдите **Public app-specific API key**
именно Google Play приложения. Он начинается с `goog_`.

В существующий файл `~/.gradle/gradle.properties` добавьте отдельной строкой, сохранив
остальные параметры:

```properties
REVENUECAT_PUBLIC_KEY=goog_ВАШ_ПУБЛИЧНЫЙ_ANDROID_SDK_KEY
```

Не добавляйте кавычки вокруг значения. Secret API key `sk_…` и service account JSON
сюда не подходят. Выполните Gradle Sync и пересоберите приложение. Пример команды:

```sh
./gradlew :app:assembleDebug
```

Для сборки на другом компьютере или в CI этот публичный параметр задаётся и там.
Само изменение Gradle properties не обновит уже установленный APK/AAB.

## 8. Подключить уведомления о возвратах

Для lifetime-продукта настройте RTDN. В Google Cloud включите Pub/Sub для того же
проекта, где создавали credentials. В настройках Google Play app в RevenueCat
найдите Real-time developer notifications, выберите/создайте topic через мастер
и скопируйте полное имя topic.

В Google Play Console → **Monetize → Monetization setup → Real-time developer
notifications** вставьте topic, включите содержимое для voided purchases и всех
one-time products, сохраните и отправьте тестовое уведомление. Проверьте получение
по текущей инструкции dashboard. Это помогает RevenueCat узнавать о возвратах
и отзыве доступа. [Инструкция RTDN](https://www.revenuecat.com/docs/platform-resources/server-notifications/google-server-notifications).

## 9. Настроить два разных списка тестировщиков

В **Internal testing → Testers** добавьте Google-аккаунт, используемый на телефоне.
Откройте opt-in ссылку этим аккаунтом, примите участие и установите приложение
через Google Play. Этот список даёт доступ к тестовой сборке.

Отдельно на уровне developer account откройте **Settings → License testing**,
добавьте тот же email в выбранный список license testers и сохраните. Эта настройка
даёт тестовые способы оплаты. Участник internal track без license testing может
получить обычную оплату.

В платёжном окне проверьте, какой Google-аккаунт выполняет покупку. При нескольких
аккаунтах это особенно важно. Для проверки выбирайте именно тестовую карту и
убедитесь, что Google пометил покупку как тестовую. Если показана обычная карта,
закройте окно и исправьте настройку license testing.

[Тестирование Google Play Billing](https://developer.android.com/google/play/billing/test),
[проверка через RevenueCat](https://www.revenuecat.com/docs/test-and-launch/sandbox/google-play-store).

## 10. Провести ручные сценарии

Сначала проверьте, что на экране Pro есть реальная локализованная цена. Затем:

| Действие | Ожидаемый результат |
| --- | --- |
| Покупка с Test card, always approves | Платёж завершается, `pro` становится active, доступны несколько автомобилей и PDF |
| Закрытие платёжного окна | Отмена; Pro не выдаётся, повторная попытка доступна |
| Test card, always declines | Ошибка; Pro не выдаётся |
| Slow test card, declines | Pending сначала, затем отмена; доступа до подтверждения нет |
| Slow test card, approves | Доступ выдаётся после подтверждения; проверьте уход из приложения и возвращение |
| Повторное открытие после успешной покупки | Pro сохраняется, повторная покупка не предлагается |
| «Восстановить покупки» | Подтверждённая магазином покупка возвращает entitlement `pro` |
| Второе устройство с тем же магазинным аккаунтом | Восстановление по покупке; локальные записи автоматически не синхронизируются |
| Возврат с отзывом покупки | После синхронизации entitlement неактивен, пользователь выбирает одно активное авто; данные сохраняются |
| Отключение сети после известной успешной покупки | Проверьте работу кешированного доступа; новая покупка требует сеть |

В RevenueCat откройте **Customers**, включите показ Sandbox и найдите проверочную
покупку по времени/тестовому клиенту. Проверьте store=Google Play, product ID,
Sandbox и active entitlement `pro`. Успешное окно оплаты без этого entitlement
не считается успешной выдачей Pro в приложении.

Для сценария переустановки сначала экспортируйте пользовательскую базу. Удаление
приложения удаляет локальные записи и согласия, поэтому не делайте это на единственной
копии своих данных. Проверьте restore/transfer policy RevenueCat для anonymous users:
приложение не содержит собственной системы аккаунтов. Восстановление покупки не
заменяет перенос базы через резервную копию.

## 11. Повторить покупку lifetime и проверить отзыв

Lifetime — non-consumable, поэтому после успеха магазин помнит владение. Для новой
полной попытки используйте Google Play Console → **Order management**, найдите
тестовый заказ и выполните **Refund and revoke**. Одна очистка данных приложения
не сбрасывает владение товаром. Дождитесь обновления RevenueCat/SDK и проверьте
отзыв, затем покупайте снова. Изменения после возврата могут распространяться
с задержкой; проверяйте обе консоли и фактический CustomerInfo.

## 12. Логи и типичные причины ошибок

В debug-сборке фильтр Logcat **`tag:DriveCareBilling`** выводит безопасные состояния:
наличие конфигурации, доступность ожидаемого lifetime package, активность entitlement
и коды ошибок покупки/restore. Ключи, чеки и платёжные данные туда не выводятся.

| Симптом | Что проверить |
| --- | --- |
| Configuration available: false | Ключ `goog_…` задан в Gradle properties, APK пересобран |
| Expected lifetime package available: false | Offering именно `default`, Lifetime package, точный product ID, активный товар и нужный магазин |
| Цена не загружается | Аккаунт/opt-in/страна/товар/сеть/credentials; дайте настройкам распространиться |
| Оплата завершилась, Pro не появился | Product привязан к entitlement именно `pro`; проверьте CustomerInfo и restore |
| Уже куплено | Для использования — восстановление, для новой полной проверки — refund and revoke |
| Invalid Play credentials | Credentials validation, права service account, совпадение package и ожидание активации |
| В sandbox работает, production нет | Проверьте production-версию товара, страну, распространение, реальный ключ приложения и серверные уведомления |

Основной путь проверки — сборка из внутреннего трека Play. Google также допускает
sideload/debug для правильно настроенных license testers с совпадающим package;
это не отменяет регистрации приложения, товара и сервисной конфигурации. Моковая
кнопка, локальное `pro=true` и отсутствие реального магазинного диалога не проверяют
интеграцию Play.

Из этого репозитория внешние аккаунты, товар, цена, credentials и тестовые платежи
не создавались и не выполнялись. Их успешность нужно подтвердить по этому списку.
