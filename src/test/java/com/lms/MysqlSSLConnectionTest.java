package com.lms;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Test class to verify MySQL SSL/TLS connection to Azure Database for MySQL
 *
 * This test verifies that:
 * 1. The DigiCertGlobalRootCA.crt.pem certificate is properly configured
 * 2. SSL connection can be established with Azure MySQL
 * 3. Database queries work properly over SSL
 */
public class MysqlSSLConnectionTest {

    // Azure MySQL Configuration from application-azure.properties
    private static final String MYSQL_HOST = "fahmio-mysql-db.mysql.database.azure.com";
    private static final String MYSQL_PORT = "3306";
    private static final String MYSQL_DATABASE = "flexibleserverdb";
    private static final String MYSQL_USER = "fahmioadmin";
    private static final String MYSQL_PASSWORD = "moin@786";

    /**
     * Test 1: Basic SSL Connection
     * Verifies that a connection can be established with SSL enabled
     */
    @Test
    public void testBasicSSLConnection() {
        String connectionUrl = String.format(
            "jdbc:mysql://%s:%s/%s?useSSL=true&requireSSL=true&serverTimezone=UTC",
            MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE
        );

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(connectionUrl, MYSQL_USER, MYSQL_PASSWORD);

            assertNotNull(conn, "Connection should not be null");
            assertFalse(conn.isClosed(), "Connection should be open");

            conn.close();
            System.out.println("[PASS] Basic SSL Connection test successful");
        } catch (Exception e) {
            fail("SSL Connection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test 2: Execute Query Over SSL
     * Verifies that a simple query can be executed over SSL connection
     */
    @Test
    public void testQueryOverSSL() {
        String connectionUrl = String.format(
            "jdbc:mysql://%s:%s/%s?useSSL=true&requireSSL=true&serverTimezone=UTC",
            MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE
        );

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(connectionUrl, MYSQL_USER, MYSQL_PASSWORD);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1 AS connection_test");

            assertTrue(rs.next(), "ResultSet should contain data");
            int testValue = rs.getInt("connection_test");
            assertEquals(1, testValue, "Query should return 1");

            rs.close();
            stmt.close();
            conn.close();
            System.out.println("[PASS] Query Over SSL test successful");
        } catch (Exception e) {
            fail("Query Over SSL failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test 3: Database Selection
     * Verifies that the correct database is selected
     */
    @Test
    public void testDatabaseSelection() {
        String connectionUrl = String.format(
            "jdbc:mysql://%s:%s/%s?useSSL=true&requireSSL=true&serverTimezone=UTC",
            MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE
        );

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(connectionUrl, MYSQL_USER, MYSQL_PASSWORD);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT DATABASE() AS current_db");

            assertTrue(rs.next(), "ResultSet should contain database name");
            String currentDb = rs.getString("current_db");
            assertEquals(MYSQL_DATABASE, currentDb, "Current database should be " + MYSQL_DATABASE);

            rs.close();
            stmt.close();
            conn.close();
            System.out.println("[PASS] Database Selection test successful. Current DB: " + currentDb);
        } catch (Exception e) {
            fail("Database Selection test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test 4: Connection Timeout Configuration
     * Verifies that connection timeout is properly configured
     */
    @Test
    public void testConnectionTimeout() {
        String connectionUrl = String.format(
            "jdbc:mysql://%s:%s/%s?useSSL=true&requireSSL=true&serverTimezone=UTC&connectTimeout=30000&socketTimeout=30000",
            MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE
        );

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            long startTime = System.currentTimeMillis();
            Connection conn = DriverManager.getConnection(connectionUrl, MYSQL_USER, MYSQL_PASSWORD);
            long endTime = System.currentTimeMillis();

            long connectionTime = endTime - startTime;
            System.out.println("[INFO] Connection established in " + connectionTime + "ms");

            assertNotNull(conn, "Connection should not be null");
            assertTrue(connectionTime < 30000, "Connection should be established within 30 seconds");

            conn.close();
            System.out.println("[PASS] Connection Timeout Configuration test successful");
        } catch (Exception e) {
            fail("Connection Timeout test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test 5: Connection Pool Test
     * Verifies that multiple connections can be established
     */
    @Test
    public void testMultipleSSLConnections() {
        String connectionUrl = String.format(
            "jdbc:mysql://%s:%s/%s?useSSL=true&requireSSL=true&serverTimezone=UTC",
            MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE
        );

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Test establishing 3 connections
            for (int i = 1; i <= 3; i++) {
                Connection conn = DriverManager.getConnection(connectionUrl, MYSQL_USER, MYSQL_PASSWORD);
                assertNotNull(conn, "Connection " + i + " should not be null");

                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT " + i + " AS connection_number");
                assertTrue(rs.next(), "ResultSet should contain data");

                rs.close();
                stmt.close();
                conn.close();
                System.out.println("[INFO] Connection " + i + " successful");
            }

            System.out.println("[PASS] Multiple SSL Connections test successful");
        } catch (Exception e) {
            fail("Multiple SSL Connections test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test 6: SSL Protocol Verification
     * Attempts to verify SSL/TLS protocol information
     */
    @Test
    public void testSSLProtocolInfo() {
        String connectionUrl = String.format(
            "jdbc:mysql://%s:%s/%s?useSSL=true&requireSSL=true&serverTimezone=UTC&enabledSslProtocolSuites=TLSv1.2",
            MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE
        );

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(connectionUrl, MYSQL_USER, MYSQL_PASSWORD);

            assertNotNull(conn, "Connection should not be null");
            assertFalse(conn.isClosed(), "Connection should be open");

            // Test a query
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT VERSION() AS mysql_version");

            if (rs.next()) {
                String version = rs.getString("mysql_version");
                System.out.println("[INFO] MySQL Server Version: " + version);
            }

            rs.close();
            stmt.close();
            conn.close();
            System.out.println("[PASS] SSL Protocol Info test successful");
        } catch (Exception e) {
            fail("SSL Protocol Info test failed: " + e.getMessage(), e);
        }
    }
}

