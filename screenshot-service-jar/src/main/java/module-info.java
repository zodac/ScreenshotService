module me.zodac {
    exports me.zodac.db;
    exports me.zodac.screenshot;
    exports me.zodac.util;

    requires me.zodac.screenshot.api;

    requires java.sql;
    requires org.apache.commons.io;
    requires org.slf4j;
    requires selenium.api;
    requires selenium.chrome.driver;
    requires selenium.remote.driver;
}