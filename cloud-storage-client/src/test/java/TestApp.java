//import lombok.Getter;
//import lombok.Setter;
//import org.junit.Test;
//import ru.geekbrains.models.Actions.Command;
//import ru.geekbrains.models.Commands;
//import ru.geekbrains.models.Message;
//import ru.geekbrains.models.Actions.Status;
//
//import java.util.*;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.function.Function;
//import java.util.function.Supplier;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//
//public class TestApp {
//    @Test
//    public void test1(){
//        List<Integer> integers = Arrays.asList(1, 2, 3);
//        List<String> strings = Arrays.asList(new String[]{"q", "w", "e","qqq"});
//        List<String> collect = IntStream.range(0, 10).mapToObj(i -> "" + i).collect(Collectors.toList());
//        strings.stream().sorted(Comparator.naturalOrder()).map(str->{
//            if (str.equals("q")){
//                return null;
//            }
//            else {
//                return str;
//            }
//        }).filter(Objects::nonNull).count();
//        integers.stream().map(intToByte);
//        String qq="w";
//        Repository repository = new Repository();
//        List<String> collect1 = strings.stream().filter(qq::equals).collect(Collectors.toList());
//        List<Boolean> collect2 = strings.stream().map(repository::existsByName).collect(Collectors.toList());
//        String collect3 = strings.stream().collect(Collectors.joining(";"));
//
//        qq="";
//    }
//
//    Function<Integer,Byte> intToByte= i->{
//        return i.byteValue();
//    };
//
//    @Test
//    public void test2(){
//        List<Message> commands = Arrays.asList(new Status("статус"), new Command("список файлов", Commands.COPY),new Status("другой статус"));
//        Map<Commands, List<Message>> collect = commands.stream().collect(Collectors.groupingBy(Message::getType, Collectors.toList()));
//        String qq="";
//    }
//
//    @Test
//    public void test3() throws Exception {
//        Optional<Integer> collect = IntStream.range(0, 10).mapToObj(i -> i).collect(Collectors.reducing((x, y) -> x + y));
//        collect.orElseThrow(()->new Exception("test"));
//        collect.orElseThrow(someError);
//        Integer sum=0;
//        Integer collect1 = IntStream.range(0, 10).mapToObj(i -> i).collect(Collectors.reducing(0, (x, y) -> x + y));
//        String qq="";
//    }
//    @Test
//    public void test4(){
//        int t=3;
//        String qq="";
//        Repository repository= new Repository();
//        List<String> strings = Arrays.asList(new String[]{"q", "w", "e","qqq"});
//        AtomicInteger ai =new AtomicInteger(0);
//        strings.stream().map(str-> str+t).collect(Collectors.toList());
//        strings.stream().map(str-> {
//            t++;
//            qq="123";
//            repository.setT(3);
//            ai.getAndIncrement();
//            return str;
//        });
//    }
//    Supplier<RuntimeException> someError = SomeErrorException::new;
//
//
//            class SomeErrorException extends RuntimeException{
//        public SomeErrorException(){
//
//        }
//            }
//    class Repository{
//                @Getter
//                @Setter
//                private int t;
//        public boolean existsByName(String name){
//            return name.length()==1;
//        }
//
//    }
//}
