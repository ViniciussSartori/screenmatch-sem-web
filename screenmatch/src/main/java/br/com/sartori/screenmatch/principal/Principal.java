package br.com.sartori.screenmatch.principal;

import br.com.sartori.screenmatch.model.DadosEpisodio;
import br.com.sartori.screenmatch.model.DadosSerie;
import br.com.sartori.screenmatch.model.DadosTemporada;
import br.com.sartori.screenmatch.model.Episodio;
import br.com.sartori.screenmatch.service.ConsumoApi;
import br.com.sartori.screenmatch.service.ConverteDados;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);

    private ConsumoApi consumo = new ConsumoApi();

    private ConverteDados conversor = new ConverteDados();

    private  final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=3f5c4015";

    public void exibeMenu(){
        System.out.println("Digite o nome da série para busca: ");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        System.out.println(dados);

        List<DadosTemporada> temporadas = new ArrayList<>();

		for (int i = 1; i<= dados.totalTemporadas(); i++){
			json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + "&season=" + i + API_KEY);
			DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
			temporadas.add(dadosTemporada);
		}
		temporadas.forEach(System.out::println);

        for (int i=0; i < dados.totalTemporadas(); i++){
            List <DadosEpisodio> episodiosTemporadas = temporadas.get(i).episodios();
            for (int j=0; j < episodiosTemporadas.size(); j++){
                System.out.println(episodiosTemporadas.get(j).titulo());
            }
        }

        //Fazendo ultimo for de uma forma melhor:
        // t: é de temporadas
        // e: é de episodios
        //episodio/temporada((parametro) -> expressao)
        temporadas.forEach(t -> t.episodios().forEach(e -> System.out.println(e.titulo())));


        //Exemplo de stream:
//        List <String> nomes = Arrays.asList("Vincius", "Ewerton", "Manu", "mafe", "Laura");
//
//        nomes.stream()
//                .sorted() // server pra ordenar alfabeticamente
//                    .limit(3)
//                        .filter(n -> n.startsWith("M"))
//                            .map(n -> n.toUpperCase())
//                                .forEach(System.out::println);


        List<DadosEpisodio> dadosEpisodios = temporadas.stream()
                .flatMap(t -> t.episodios().stream()) // flatMap: transforma cada elemento em uma lista/stream e junta tudo em uma única sequência
                    .collect(Collectors.toList()); // collect(Collectors.toList()): finaliza o stream e junta os elementos em uma List

        System.out.println("\nTop 5 espisódios:");
        dadosEpisodios.stream()
                .filter(e -> !e.avaliacao().equalsIgnoreCase("N/A")) // filter(!avaliacao == "N/A"): remove os episódios sem avaliação válida ("N/A"), sem diferenciar maiúsculas/minúsculas
                    .sorted(Comparator.comparing(DadosEpisodio::avaliacao).reversed()) // sorted(...reversed()): ordena os episódios pela avaliação, em ordem decrescente
                        .limit(5)
                            .forEach(System.out::println);


        List <Episodio> episodios = temporadas.stream()
                .flatMap(t -> t.episodios().stream() // flatMap(t -> t.episodios().stream()): une os episódios de todas as temporadas em um único stream
                        .map(d -> new Episodio(t.numero(), d))) // map(d -> new Episodio(t.numero(), d)): converte cada DadosEpisodio em um Episodio, guardando o número da temporada
                            .collect(Collectors.toList());

        episodios.forEach(System.out::println);

        System.out.println("A partir de que ano você deseja ver os episódios?");
        var ano = leitura.nextInt();
        leitura.nextLine();

        LocalDate dataBusca = LocalDate.of(ano, 1 , 1);

        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy"); // DateTimeFormatter.ofPattern("dd/MM/yyyy"): define o formato de data brasileiro (dia/mês/ano) para exibir e ler datas

        episodios.stream()
                .filter(e -> e.getDataLancamento() != null && e.getDataLancamento().isAfter(dataBusca)) // filter(!= null && isAfter(dataBusca)): mantém só episódios com data de lançamento válida e posterior à data informada
                .forEach(e -> System.out.println(
                        "Temporada: " + e.getTemporada() +
                                " Episódio: " + e.getTitulo() +
                                " Data de Lancamento: " + e.getDataLancamento().format(formatador) // Monta o texto de saída: rótulo + data de lançamento formatada com o formatador dd/MM/yyyy
                ));

    }

}
