//package com.lab.statistics;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.*;
import javax.swing.text.html.Option;

import static java.util.stream.Collectors.groupingBy;

public class MainFrame {
    private JFrame frame;
    // ścieżka do katalogu z plikami tekstowymi
    private static final String DIPR_ATH = "C:\\Users\\admin\\IdeaProjects\\Lab1\\src\\words";
    // określa ile najczęściej występujących wyrazów bierzemy pod uwagę
    private final int liczbaWyrazowStatystyki;
    private final AtomicBoolean fajrant;
    private final int liczbaProducentow;
    private final int liczbaKonsumentow;
    // pula wątków – obiekt klasy ExecutorService, który zarządza tworzeniem
    // nowych oraz wykonuje 'recykling' zakończonych wątków
    private ExecutorService executor;
    // lista obiektów klasy Future, dzięki którym mamy możliwość nadzoru pracy wątków
    // producenckich np. sprawdzania czy wątek jest aktywny lub jego anulowania/przerywania
    private List<Future<?>> producentFuture;


    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MainFrame window = new MainFrame();
                    window.frame.pack();
                    window.frame.setAlwaysOnTop(true);
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public MainFrame() {
        liczbaWyrazowStatystyki = 5;
        fajrant = new AtomicBoolean(false);
        liczbaProducentow = 1;
        liczbaKonsumentow = 2;
        executor = Executors.newFixedThreadPool(liczbaProducentow + liczbaKonsumentow);
        producentFuture = new ArrayList<>();
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frame = new JFrame();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                executor.shutdownNow();
            }
        });
        frame.setBounds(100, 100, 450, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        frame.getContentPane().add(panel, BorderLayout.NORTH);
        JButton btnStop = new JButton("Stop");
        btnStop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fajrant.set(true);
                for (Future<?> f : producentFuture) {
                    f.cancel(true);
                }
            }
        });
        JButton btnStart = new JButton("Start");
        btnStart.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                multiThreadedStatistics();
            }
        });
        JButton btnZamknij = new JButton("Zamknij");
        btnZamknij.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                executor.shutdownNow();
                System.exit(0);
            }
        });

        panel.add(btnStart);
        panel.add(btnStop);
        panel.add(btnZamknij);
    }

    /**
     * Statystyka wyrazów (wzorzec PRODUCENT - KONSUMENT korzystający z kolejki blokującej)
     */
    private void multiThreadedStatistics() {
        for (Future<?> f : producentFuture) {
            if (!f.isDone()) {
                JOptionPane.showMessageDialog(frame, "Nie można uruchomić nowego zadania! " +
                        "Przynajmniej jeden producent nadal działa!", "OSTRZEŻENIE", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        fajrant.set(false);
        producentFuture.clear();
        final BlockingQueue<Optional<Path>> kolejka = new LinkedBlockingQueue<>(liczbaKonsumentow);
        final int przerwa = 60;
        Runnable producent = () -> {
            final String name = Thread.currentThread().getName();
            String info = String.format("PRODUCENT %s URUCHOMIONY ...", name);
            System.out.println(info);
            while (!Thread.currentThread().isInterrupted()) {
                if (fajrant.get()) {
                    // TODO przekazanie poison pills (kolejka.put(Optional.empty());) konsumentom i zakończenia działania
                    try {
                        Optional<Path> poisonPill = Optional.empty();
                        kolejka.put(poisonPill);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                } else {
                    // TODO Wyszukiwanie plików *.txt i wstawianie do kolejki ścieżki opakowanej w Optional (Optional<Path>
                    // optPath = Optional.ofNullable(path); kolejka.put(optPath);) lub oczekiwanie jeśli kolejka
                    // pełna. Do wyszukiwania plików można użyć metody Files.walkFileTree oraz klasy SimpleFileVisitor<Path>

                    try {
                        Path searchDestination = Paths.get(DIPR_ATH);

                        SimpleFileVisitor<Path> fileVisitor = new SimpleFileVisitor<Path>()
                        {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                // Action to perform for each file
                                System.out.println("File: " + file);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                                // Action to perform before visiting a directory
                                System.out.println("Directory: " + dir);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                                // Action to perform if there is an error visiting a file
                                System.err.println("Unable to visit file: " + file + " (" + exc.getMessage() + ")");
                                return FileVisitResult.CONTINUE;
                            }
                        };
                        var path = Files.walkFileTree(searchDestination, fileVisitor);
                        var optPath = Optional.ofNullable(path);
                        kolejka.put(optPath);

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                info = String.format("Producent %s ponownie sprawdzi katalogi za %d sekund", name, przerwa);
                System.out.println(info);
                try {
                    TimeUnit.SECONDS.sleep(przerwa);
                } catch (InterruptedException e) {
                    info = String.format("Przerwa producenta %s przerwana!", name);
                    System.out.println(info);
                    if (!fajrant.get()) Thread.currentThread().interrupt();
                }
            }
            info = String.format("PRODUCENT %s SKOŃCZYŁ PRACĘ", name);
            System.out.println(info);
        };
        Runnable konsument = () -> {
            final String name = Thread.currentThread().getName();
            String info = String.format("KONSUMENT %s URUCHOMIONY ...", name);
            System.out.println(info);
            Map<String, Long> result = null;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // TODO pobieranie ścieżki (Optional<Path> optPath = kolejka.take();) i tworzenie statystyki wyrazów
                    // lub oczekiwanie jeśli kolejka jest pusta. Brak ścieżki tj. !optPath.isPresent() oznacza koniec pracy
                    Optional<Path> optPath = kolejka.take();
                    if (!optPath.isPresent()) {
                        throw new InterruptedException();
                    }
                    result = getLinkedCountedWords(optPath.get(), liczbaWyrazowStatystyki);

                    if (!result.isEmpty()){
                        JPanel resultPanel = new JPanel();
                        frame.getContentPane().add(resultPanel, BorderLayout.SOUTH);

                        DefaultListModel<String> listModel = new DefaultListModel<>();
                        List<String> formattedList = new ArrayList<>();
                        for (Map.Entry<String, Long> entry : result.entrySet()) {
                            formattedList.add(entry.getKey() + " - " + entry.getValue());
                        }

                        formattedList.forEach(listModel::addElement);

                        JList<String> titleValueJList = new JList<String>(listModel);
                        JScrollPane scrollPane = new JScrollPane(titleValueJList);
                        resultPanel.add(scrollPane);
                        frame.repaint();
                    }

                } catch (InterruptedException e) {
                    info = String.format("Oczekiwanie konsumenta %s na nowy element z kolejki przerwane!", name);
                    System.out.println(info);
                    Thread.currentThread().interrupt();
                }
            }

            info = String.format("KONSUMENT %s ZAKOŃCZYŁ PRACĘ", name);
            System.out.println(info);
        };
        //uruchamianie wszystkich wątków-producentów
        for (int i = 0; i < liczbaProducentow; i++) {
            Future<?> pf = executor.submit(producent);
            producentFuture.add(pf);
        }
        //uruchamianie wszystkich wątków-konsumentów
        for (int i = 0; i < liczbaKonsumentow; i++) {
            executor.execute(konsument);
        }
    }

    /**
     * Metoda zwraca najczęściej występujące słowa (ich liczbę określa wordsLimit, a słowa są sortowane względem
     * częstotliwości ich występowania) we wskazanym pliku tekstowym
     */

    private Map<String, Long> getLinkedCountedWords(Path path, int wordsLimit) {
        //konstrukcja 'try-with-resources' - z automatycznym zamykaniem strumienia/źródła danych
        try (BufferedReader reader = Files.newBufferedReader(path)) {// wersja ze wskazaniem kodowania
            // Files.newBufferedReader(path, StandardCharsets.UTF_8)
            return reader.lines() // można też bez buforowania - Files.readAllLines(path)
                    //TODO
                    // 1. podział linii na słowa, można skorzystać z funkcji map, metody split i wyrażenia regularnego np. "\\s+"
                    // oznaczającego jeden lub więcej tzw. białych znaków. Po tej operacji pewnie będzie potrzebna konwersja
                    // strumienia 'String[]' do strumienia 'String' za pomocą .flatMap(Arrays::stream)
                    // 2. wycięcie wszystkich znaków, które nie tworzą słów m.in. ;,.?!:
                    // Można użyć np. .map(word -> word.replaceAll("[^a-zA-Z0-9ąęóśćżńźĄĘÓŚĆŻŃŹ]{1}", ""))
                    // 3. filtrowanie słów - tylko z przynajmniej trzema znakami, użyj funkcji filter, metody matches i wyrażenia
                    // regularnego w lambdzie np. word -> word.matches("[a-zA-Z0-9ąęóśćżńźĄĘÓŚĆŻŃŹ]{3,}")
                    // (bez uwzględniania polskich znaków byłoby "[a-zA-Z]{3,}")
                    // 4. konwersja do małych liter, aby porównywanie słów było niewrażliwe na wielkość liter (można zrobić wcześniej,
                    // przed p. 2, wtedy wyrażenia regularne nie będą musiały uwzględniać wielkich literek)
                    // 5. grupowanie słów względem liczebności ich występowania, można użyć
                    // funkcji collect z Collectors.groupingBy(Function.identity(), Collectors.counting()),
                    // po tej operacji należy zrobić konwersję na strumień tj. .entrySet().stream()
                    // 6. sortowanie względem przechowywanych w mapie wartości, w kolejności malejącej,
                    // można użyć Map.Entry.comparingByValue(Comparator.reverseOrder())
                    // 7. ograniczenie liczby słów do wartości z wordsLimit
                    .flatMap(line -> Arrays.stream(line.split("\\s+")))
                    .map(word -> word.replaceAll("[^a-zA-Z0-9ąęóśćżńźĄĘÓŚĆŻŃŹ]{1}", ""))
                    .filter(word -> word.matches("[a-zA-Z0-9ąęóśćżńźĄĘÓŚĆŻŃŹ]{3,}"))
                    .map(String::toLowerCase)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                    .entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(wordsLimit)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyMap();

        }
    }
}