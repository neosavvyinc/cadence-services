import com.github.bigtoast.sbtliquibase.LiquibasePlugin

seq(LiquibasePlugin.liquibaseSettings: _*)

liquibaseUsername := "cadence"

liquibasePassword := "cadence"

liquibaseDriver   := "com.mysql.jdbc.Driver"

liquibaseUrl      := "jdbc:mysql://localhost:3306/cadence"
