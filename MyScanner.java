import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
 
/**
 * My simple scanner which can parse input from readers, input streams and plain strings and also
 * convert it to ints. With each call of next... method it fetches new token (consecutive sequence
 * of non-whitespace chars) and returns previously fetched one.
 *
 * @author Serafim Chukov
 */
public class MyScanner implements AutoCloseable {
  private final Reader reader;
  private final String lineSeparator;
  private final SeparatorChecker checker;
  private String lastToken;
  private int skippedLines;
  private int lastRead = ' ';
 
  /**
   * Creates new instance of MyScanner class using {@link java.io.BufferedReader} over provided
   * reader and fetches new token for the next call of {@code next()} or {@code nextInt()}.
   *
   * @param reader an underlying reader
   * @throws IOException if an I/O error occurred while trying to fetch next token
   */
  private MyScanner(Reader reader, SeparatorChecker checker, String lineSeparator)
      throws IOException {
    this.reader = new BufferedReader(reader);
    this.checker = checker;
    this.lineSeparator = lineSeparator;
    fetchNextToken();
  }
 
  /**
   * Fetches new token, where "token" is a continuous non-whitespace character sequence.
   *
   * @throws IOException if I/O error occurs while reading
   */
  private void fetchNextToken() throws IOException {
    int read = lastRead;
    StringBuilder nextToken = new StringBuilder();
    skippedLines = 0;
    do {
      char c = (char) read;
 
      if (checker.isSeparator(c)) {
        if (nextToken.length() != 0) {
          lastToken = nextToken.toString();
          lastRead = read;
          return;
        }
 
        if (c == lineSeparator.charAt(0)) {
          reader.mark(lineSeparator.length() + 1);
          skippedLines += 1;
          for (int i = 1; i < lineSeparator.length(); i++) {
            read = reader.read();
            if (read == -1 || (((char) read) != lineSeparator.charAt(i))) {
              reader.reset();
              skippedLines -= 1;
              break;
            }
          }
        }
      } else {
        nextToken.append(c);
      }
    } while ((read = reader.read()) != -1);
 
    if (nextToken.length() == 0) {
      lastToken = null;
    } else {
      lastToken = nextToken.toString();
    }
  }
 
  /**
   * Fetches new token and returns previous token or {@code null} if MyScanner is at the end.
   *
   * @return previously fetched token or {@code null} if no more tokens were fetched (see {@code
   *     hasNext()})
   * @throws IOException if an I/O error occurs when fetching next token
   */
  public String next() throws IOException {
    String rv = lastToken;
    fetchNextToken();
    return rv;
  }
 
  /**
   * Fetches new token and returns previous token as an int. Similar to {@code
   * Integer.parseInt(next())}.
   *
   * @return previously fetched token as an int
   * @throws IOException if an I/O error occurs when fetching next token
   * @throws NumberFormatException if failed to parse token as an int
   * @throws NullPointerException if called when no more tokens were fetched
   */
  public int nextInt() throws IOException {
    return Integer.parseInt(next());
  }
 
  /**
   * Fetches new token and returns int value of previous token represented as a hexadecimal number
   * of format 0x...
   *
   * @return previously fetched token as an int
   * @throws IOException if an I/O error occurs when fetching next token
   * @throws NumberFormatException if failed to parse token as a hexadecimal of format 0x...
   * @throws NullPointerException if called when no more tokens were fetched
   */
  public int nextHex() throws IOException {
    if (lastToken.length() <= 2)
      throw new NumberFormatException(lastToken + " is not a hex number in format 0x...");
    return parseHex(next().toLowerCase().substring(2));
  }
 
  private int parseHex(String hex) {
    try {
      return Integer.parseInt(hex, 16);
    } catch (NumberFormatException e) {
      return Integer.parseInt(complementHex(hex), 16) - 1;
    }
  }
 
  private String complementHex(String hex) {
    StringBuilder comp = new StringBuilder("-");
    for (int i = 0; i < hex.length(); i++) {
      char c = hex.charAt(i);
      if (c <= '5' || c >= 'a') {
        comp.append((char) ('f' + '0' - c));
      } else {
        comp.append((char) ('8' + '7' - c));
      }
    }
    return comp.toString();
  }
 
  /** @return {@code true} if next returned token starts on new line */
  public boolean onNewLine() {
    return skippedLines > 0;
  }
 
  /** @return number of lineSeparator sequences that will occur before next token */
  public int getSkippedLines() {
    return skippedLines;
  }
 
  /** @return {@code true} if this instance of MyScanner has more tokens to provide */
  public boolean hasNext() {
    return lastToken != null;
  }
 
  /** @return {@code true} if next token exists and can be returned as integer */
  public boolean hasNextInt() {
    if (!hasNext()) {
      return false;
    }
    try {
      Integer.parseInt(lastToken);
    } catch (NumberFormatException ignored) {
      return false;
    }
    return true;
  }
 
  /** @return {@code true} if next token exists and is a hexadecimal int number of format 0x... */
  public boolean hasNextHex() {
    if (!hasNext() || lastToken.length() <= 2 || !lastToken.toLowerCase().startsWith("0x")) {
      return false;
    }
    try {
      parseHex(lastToken.substring(2));
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }
 
  public void close() throws IOException {
    reader.close();
  }
 
  public interface SeparatorChecker {
    boolean isSeparator(char c);
  }
 
  public static class Builder {
    private final Reader reader;
    private SeparatorChecker checker = Character::isWhitespace;
    private String lineSeparator = System.lineSeparator();
 
    public Builder(InputStream is) {
      this.reader = new InputStreamReader(is);
    }
 
    public Builder(Reader r) {
      this.reader = r;
    }
 
    public Builder setLineSeparator(String lineSeparator) {
      this.lineSeparator = lineSeparator;
      return this;
    }
 
    public Builder setSeparatorChecker(SeparatorChecker checker) {
      this.checker = checker;
      return this;
    }
 
    public MyScanner build() throws IOException {
      return new MyScanner(reader, checker, lineSeparator);
    }
  }
}

