@Grab('org.yaml:snakeyaml:1.23')
import org.yaml.snakeyaml.Yaml

import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import groovy.sql.Sql
import com.informix.jdbc.IfxDriver

Yaml parser = new Yaml()
def sf = new SimpleDateFormat("dd.MM.yyyy HH:mm:SSS")
def config = parser.loadAs(("src/upgrade.yml" as File).text, Map)
def version = config.upgrade.version

File files = 'changed_files.chg' as File
def t = files.text
try {
    def fileList = []
    fileList = config.upgrade.files
    if (!fileList.empty) {
        files.delete()
        files.createNewFile()
        fileList.each {
            files << "$it\n"
        }
        if (t.contains("upgrade.yml"))
            files << "\nupgrade.yml"
    }
}
catch (Exception ex) {


}
def upgradeScript = "./scr$version" as File
if (upgradeScript.isFile()) upgradeScript.delete()
upgradeScript << "# UPGRADE $version SHARE\n"
//upgradeScript << "## DATE : ${sf.format(new Date())}\n"
upgradeScript << "\n"
config.upgrade.target.each {
    upgradeScript << "#ADDRESS \$LIST_DBNAME ${it.key}  ${it.value}\n"
}
upgradeScript << "\n"


if (!files.text.contains('upgrade.yml')) throw new RuntimeException("Please, execute upgrade.groovy!!")

def upgrDir = new File("upgrade")
upgrDir.mkdirs()
files.readLines().each {
    def file = new File(it)
    try {
        if (!it.contains('upgrade.yml')) {
            Files.copy(Paths.get(file.canonicalPath), Paths.get(new File(upgrDir.canonicalPath, file.name).canonicalPath))
            upgradeScript << "makesql \$LIST_DBNAME $it\n"
        }
    }
    catch (Exception ex) {
    }
}

Files.copy(Paths.get(upgradeScript.canonicalPath), Paths.get(new File(upgrDir.canonicalPath, upgradeScript.name).canonicalPath))

def upgr = new File("upgr/${version}")
upgr.mkdirs()

"tar -C ${upgrDir.canonicalPath} -cvf ${upgr.canonicalPath}/upgr${version}.tar .".execute()

"scp -r ${upgr.canonicalPath} root@45.55.43.205:/root/upgr".execute()

def sql = Sql.newInstance('jdbc:informix-sqli://45.55.43.205:9088/mydb:INFORMIXSERVER=informix', 'sabbath', 'sabbath', 'com.informix.jdbc.IfxDriver')
def upgradeName = "upgr${version}.tar"
sql.execute "insert into updates(filename) values ('$version')"

println "=============> OK <================"