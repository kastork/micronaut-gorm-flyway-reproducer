## Problem reproducer for Micronaut with Gorm and Flyway

### trying to emulate the suggestions in the mn flyway docs.

```yaml
jpa:
  default:
    properties:
      hbm2ddl:
        auto: none
      show_sql: true
    
datasources:
  default:
    url: jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE
    pooled: true
    jmxExport: true
    dbCreate: none
    driverClassName: org.h2.Driver
    username: sa
    password: ''
  
flyway:
  datasources:
    default:
      enabled: true
      locations: classpath:db

```


```bash
11:19:06.231 [main] DEBUG i.m.context.condition.Condition - * Flyway bean not created for identifier [default] because no data source was found with a named qualifier of the same name.

```

### Trying to emulate how the default gorm project configures things

```yaml
jpa:
  properties:
    hbm2ddl:
      auto: none
    show_sql: true
    
dataSource:
  url: jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE
  pooled: true
  jmxExport: true
  dbCreate: none
  driverClassName: org.h2.Driver
  username: sa
  password: ''
  
flyway:
  enabled: true
  locations: classpath:db

```

The `BeanCreatedEvent` for `FlywayConfigurationProperties` bean is never heard by `AlternativeMigrationRunner` as shown with a breakpoint set in the `onCreated` method of that class.

### Grasping at Straws

```yaml
flyway:
  dataSource:
    enabled: true
    locations: classpath:db
```

Again, no `BeanCreatedEvent` is heard.

```yaml
flyway:
  datasources:
    default:
      enabled: true
      locations: classpath:db
```

Same

`applicationContext.containsBean(DataSource.class, Qualifiers.byName(""))`

Then I noticed that the Gorm default data source is named "".

So I tried

```yaml
flyway:
  datasources:
    "":
      enabled: true
      locations: classpath:db
```

And, lo, the flyway config accepted that.

However, in `DataSourceMigrationRunner`, the `DataSource` bean doesn't pass the test.

```java
    @Override
    public DataSource onCreated(BeanCreatedEvent<DataSource> event) {
        DataSource dataSource = event.getBean();

        if (event.getBeanDefinition() instanceof NameResolver) {
            ((NameResolver) event.getBeanDefinition())
                    .resolveName()
                    .ifPresent(name -> {
                        applicationContext
                                .findBean(FlywayConfigurationProperties.class, Qualifiers.byName(name))
                                .ifPresent(flywayConfig -> run(flywayConfig, dataSource));
                    });
        }

        return dataSource;
    }
```

so the migration isn't run.

### Better Gorm config, maybe

```yaml
dataSource:
#  url: jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE
  pooled: true
  jmxExport: true
  dbCreate: none
  driverClassName: org.h2.Driver
  username: sa
  password: ''
  
datasources:
  default:
    url: jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE

flyway:
  datasources:
    default:
      enabled: true
      locations: classpath:db
``` 

Now, we do get a DataSource that has a name qualifier of "default".

But the `DataSourceMigrationRunner` still does not recognize the datasource as being a `NameResolver`