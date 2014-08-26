import com.github.bigtoast.sbtliquibase.LiquibasePlugin

seq(LiquibasePlugin.liquibaseSettings: _*)

liquibaseUsername := "cadencev1"

liquibasePassword := "cadencev1"

liquibaseDriver   := "com.mysql.jdbc.Driver"

liquibaseUrl      := "jdbc:mysql://localhost:3306/cadencev1"
