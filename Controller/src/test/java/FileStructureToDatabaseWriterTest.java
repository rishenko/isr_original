import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.astrodoorways.db.filesystem.FileInfo;
import com.astrodoorways.db.filesystem.BaseFileInfoDAO;
import com.astrodoorways.db.filesystem.FileInfoDAO;
import com.astrodoorways.db.filesystem.BaseFileStructureToDatabaseWriter;

@RunWith(JUnit4.class)
public class FileStructureToDatabaseWriterTest {

	@Test
	public void test() {
		FileInfo dto = new FileInfo();
		FileInfoDAO dao = new BaseFileInfoDAO();
		dto.setFileName("name");
		dto.setDirectory("dir");
		dto.setExtension("ext");
		dao.saveFilePath(dto);
	}

	@Test
	public void filetoDatabaseTest() throws IOException {
		BaseFileStructureToDatabaseWriter writer = new BaseFileStructureToDatabaseWriter();
		writer.writeFileStructure(new File("src/test/resources/test-dirs/read/data/cassini"));
		assertTrue(writer.getCollectionOfFiles().size() > 0);
	}
}
