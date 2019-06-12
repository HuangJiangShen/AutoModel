package com.shj.auto;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AutoModel {
    private static String driver = "com.mysql.cj.jdbc.Driver";
    private static String tableSchema = "dc_learning_platform";//库名
    private static String url = "jdbc:mysql://localhost:3306/" +tableSchema;
    private static String username = "root";
    private static String password = "123456";
    private static List<String> tableNameList = new ArrayList<>();
    private static String sql = "";
    private static ResultSet resultSet = null;
    private static String path = new AutoModel().getClass().getResource("/").getFile().toString();//可以自行指定生成路径
    private static String packageName = "com.shj.jdbc";//包名
    private static String packageStr = "package packageName;\n";//包名
    private static String headerStr = "\n@Data\n@ApiModel(value = \"XX模块Model\")\npublic class ClassName {\n";//类的头
    private static String inportJar = "\nimport io.swagger.annotations.ApiModel;\nimport io.swagger.annotations.ApiModelProperty;\nimport lombok.Data;\n";//引入的jar
    private static String lastStr = "\n}";//类的尾
    private static boolean delPre = true;//是否去除前缀前缀
    private static String preStr = "t_";//前缀字符

    public static void main(String args[]) throws Exception {

        //查询数据库所有的表名
        sql = "select table_name from information_schema.Columns where  table_schema='"+tableSchema+"' GROUP BY TABLE_NAME";
        resultSet = select(sql);
        int col = resultSet.getMetaData().getColumnCount();
        while (resultSet.next()) {
            for (int i = 1; i <= col; i++) {
                tableNameList.add(resultSet.getString(i));
            }
        }
        System.out.println(tableNameList);
        for (int i = 0; i < tableNameList.size(); i++) {
            String tabelName = tableNameList.get(i);
            //遍历查询表字段以及注释以及字段类型等
            sql = "select column_name,column_comment,column_type,column_key from information_schema.Columns " +
                    "where  table_schema='"+tableSchema+"' and TABLE_NAME='" + tabelName + "'";
            resultSet = select(sql);
            String content = "";
            while (resultSet.next()) {
                content = content + dealContent(resultSet.getString("column_name").replaceAll("\n", "")
                        , resultSet.getString("column_type").replaceAll("\n", "")
                        , resultSet.getString("column_comment").replaceAll("\n", "")
                );
            }
            boolean flag = false;
            tabelName = dealColumnName(dealTableName(tabelName));
            String filePath = path + tabelName + ".java";
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
                flag = writeFileContent(filePath, packageStr.replace("packageName", packageName) + inportJar + headerStr.replace("ClassName", tabelName) + content + lastStr);
            }
            if(flag){
                System.out.println(tabelName+"；类创建成功");
            }else{
                System.out.println(tabelName+"；类创建失败===请注意！！！");
            }


        }
    }

    /**
     * @param columeName    列名
     * @param columeType    列类型
     * @param columnComment 列注释
     * @return
     */
    private static String dealContent(String columeName, String columeType, String columnComment) {
        String sg = "\n   @ApiModelProperty(value = \"" + columnComment + "\")\n";
        return sg + "   private " + dealColumnType(columeType) + " " + dealColumnName(columeName) + ";\n";
    }

    /**
     * 处理常用字段类型
     */
    private static String dealColumnType(String type) {
        if (type.indexOf("int") > -1) {
            return "int";
        }
        if (type.indexOf("varchar") > -1) {
            return "String";
        }
        if (type.indexOf("datetime") > -1) {
            if (!(inportJar.indexOf("import java.util.Date;") > -1)) {
                inportJar += "import java.util.Date;\n";
            }
            return "Date";
        }
        if (type.indexOf("decimal") > -1) {
            if (!(inportJar.indexOf("import java.math.BigDecimal;") > -1)) {
                inportJar += "import java.math.BigDecimal;\n";
            }
            return "BigDecimal";
        }
        if (type.indexOf("char") > -1) {
            return "String";
        }
        if (type.indexOf("float") > -1) {
            return "float";
        }
        if (type.indexOf("double") > -1) {
            return "double";
        }
        //待补充，只放类常用的

        return "String";
    }

    /**
     * 处理字段名，主要是将下划线去掉，并设置后面的字母大写，即驼峰形式（如user_name处理成userName）
     */
    private static String dealColumnName(String columnName) {
        int fromIndex = 0;
        int index;
        while (fromIndex < columnName.length()) {
            index = columnName.indexOf("_", fromIndex);
            if (index >= 0) {
                columnName = columnName.substring(0, index) + columnName.substring(index + 1, index + 2).toUpperCase() + columnName.substring(index + 2);
                fromIndex = index;
            } else {
                break;
            }

        }
        return columnName;
    }


    /**
     * 表名前缀处理，去掉前缀，并设置类名首字母大写；如t_class处理成Class
     */
    private static String dealTableName(String pre) {
        pre = pre.replaceFirst(preStr, "");
        pre = pre.substring(0, 1).toUpperCase() + pre.substring(1);
        //保存字段处理，防止冲突，待完善
        if (pre.equals("Class")) {
            pre = "Clazz";
        }
        return pre;
    }

    /**
     * 获取链接
     */
    private static Connection getConn() {
        Connection conn = null;
        try {
            Class.forName(driver); //classLoader
            conn = (Connection) DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    /**
     * 查询sql
     */
    private static ResultSet select(String sql) throws Exception {
        Connection conn = getConn();
        PreparedStatement pstmt;
        try {
            pstmt = (PreparedStatement) conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            return rs;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 写入文件
     */
    public static boolean writeFileContent(String filepath, String newstr) throws IOException {
        Boolean bool = false;
        String filein = newstr + "\r\n";//新写入的行，换行
        String temp = "";

        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        FileOutputStream fos = null;
        PrintWriter pw = null;
        try {
            File file = new File(filepath);//文件路径(包括文件名称)
            //将文件读入输入流
            fis = new FileInputStream(file);
            isr = new InputStreamReader(fis);
            br = new BufferedReader(isr);
            StringBuffer buffer = new StringBuffer();

            //文件原有内容
            for (int i = 0; (temp = br.readLine()) != null; i++) {
                buffer.append(temp);
                // 行与行之间的分隔符 相当于“\n”
                buffer = buffer.append(System.getProperty("line.separator"));
            }
            buffer.append(filein);

            fos = new FileOutputStream(file);
            pw = new PrintWriter(fos);
            pw.write(buffer.toString().toCharArray());
            pw.flush();
            bool = true;
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        } finally {
            //不要忘记关闭
            if (pw != null) {
                pw.close();
            }
            if (fos != null) {
                fos.close();
            }
            if (br != null) {
                br.close();
            }
            if (isr != null) {
                isr.close();
            }
            if (fis != null) {
                fis.close();
            }
        }
        return bool;
    }

}
