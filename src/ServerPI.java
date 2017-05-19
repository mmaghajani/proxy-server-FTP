import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by mma on 5/18/17.
 */
public class ServerPI implements Runnable {
    private Socket connection;
    private boolean loggedIn = false;
    private String username = "";
    private String password = "";

    public ServerPI(Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        BufferedReader inFromClient = null;
        try {
            inFromClient = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(
                    connection.getOutputStream());
            outerLoop:while (true) {
                String clientSentece = inFromClient.readLine();
                String command = clientSentece.split("\\s")[0];
                String parameter = "";
                if( (clientSentece.split("\\s")).length > 1)
                    parameter = clientSentece.split("\\s")[1];
                switch (command) {
                    case "USER":
                        if(!loggedIn) {
                            username = parameter;
                            if (username.equals("root"))
                                outToClient.writeBytes("331 " + Constants.PASSWORD_REQUIRED);
                            else
                                outToClient.writeBytes("332 " + Constants.NEED_ACCOUNT);
                        }else{
                            outToClient.writeBytes("230 " + Constants.LOGGED_IN);
                        }
                        break;
                    case "PASS":
                        if(!loggedIn) {
                            password = parameter;
                            if (username.equals("root") && password.equals("toor")) {
                                outToClient.writeBytes("230 " + Constants.LOGGED_IN);
                                loggedIn = true;
                            } else if (username.equals("root") && !password.equals("toor")) {
                                username = "";
                                outToClient.writeBytes("331 " + Constants.PASSWORD_REQUIRED);
                            } else {
                                outToClient.writeBytes("332 " + Constants.NEED_ACCOUNT);
                            }
                        }else{
                            outToClient.writeBytes("230 " + Constants.LOGGED_IN);
                        }
                        break;
                    case "RMD":
                        if (loggedIn){
                            File index = new File("/files");
                            String[]entries = index.list();
                            for(String s: entries){
                                File currentFile = new File(index.getPath(),s);
                                currentFile.delete();
                            }
                            outToClient.writeBytes("200 " + Constants.OK);
                        }else{
                            outToClient.writeBytes("530 " + Constants.NOT_LOGGED_IN);
                        }
                        break;
                    case "DELE":
                        if(loggedIn){
                            String filename = parameter;
                            File index = new File("/files/" + filename);
                            if(index.exists()) {
                                index.delete();
                                outToClient.writeBytes("200 " + Constants.OK);
                            }else{
                                outToClient.writeBytes("553 " + Constants.FILE_NAME_NOT_ALLOWED);
                            }
                        }else{
                            outToClient.writeBytes("530 " + Constants.NOT_LOGGED_IN);
                        }
                        break;
                    case "RETR":
                        if(loggedIn){
                            String filename = parameter;
                            File index = new File("/file/" + filename);
                            if(index.exists()){
                                outToClient.writeBytes("200 " + Constants.OK);
                                ServerDTP serverDTP = new ServerDTP(connection);
                                serverDTP.sendFile(index);
                            }else{
                                String request = "GET /~94131090/CN1_Project_Files/" + filename +
                                        " HTTP/1.1\r\n" +
                                        "Host: ceit.aut.ac.ir:80\r\n" +
                                        "Connection: Close\r\n"+
                                        "\r\n";
                                String response = sendHTTPRequestToServer(request);
                                String contentType = getContentTypeFromResponse(response);
                                String body = getBodyFromResponse(response);
                                File file = new File("/files/" + filename+"."+contentType);
                                file.createNewFile();
                                PrintWriter out = new PrintWriter(new FileOutputStream(file));
                                out.write(body);
                                if(body != null ) {
                                    outToClient.writeBytes("200 " + Constants.OK);
                                    ServerDTP serverDTP = new ServerDTP(connection);
                                    serverDTP.sendFile(file);
                                }else{
                                    outToClient.writeBytes("425 " + Constants.CANT_OPEN_DATA_CONNECTION);
                                }
                            }
                        }else{
                            outToClient.writeBytes("530 " + Constants.NOT_LOGGED_IN);
                        }
                        break;
                    case "LIST":
                        if(loggedIn){
                            String request = "GET /~94131090/CN1_Project_Files/ HTTP/1.1\r\n" +
                                    "Host: ceit.aut.ac.ir:80\r\n" +
                                    "Connection: Close\r\n"+
                                    "\r\n";
                            String response = sendHTTPRequestToServer(request);
                            if(response != null ) {
                                outToClient.writeBytes("200 " + Constants.OK);
                                ServerDTP serverDTP = new ServerDTP(connection);
                                serverDTP.sendString(response);
                            }else{
                                outToClient.writeBytes("425 " + Constants.CANT_OPEN_DATA_CONNECTION);
                            }
                        }else{
                            outToClient.writeBytes("530 " + Constants.NOT_LOGGED_IN);
                        }
                        break;
                    case "QUIT":
                        connection.close();
                        break outerLoop;
                    default:
                        outToClient.writeBytes("502 " + Constants.COMMAND_NOT_IMPLEMENTED);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String getBodyFromResponse(String response) {
        return "ÿØÿà\u0010JFIF\u0001\u0001\u0001\u0001ÿþ;CREATOR: gd-jpeg v1.0 (using IJG JPEG v62), quality = 95\n" +
                "ÿÛC\u0002\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0001\u0001\u0002\u0002\u0002\u0002\u0002\u0004\u0003\u0002\u0002\u0002\u0002\u0005\u0004\u0004\u0003\u0004\u0006\u0005\u0006\u0006\u0006\u0005\u0006\u0006\u0006\u0007\t\b\u0006\u0007\t\u0007\u0006\u0006\b\u000B\b\t\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\u0006\b\u000B\f\u000B\n" +
                "\f\t\n" +
                "\n" +
                "\n" +
                "ÿÛC\u0001\u0002\u0002\u0002\u0002\u0002\u0002\u0005\u0003\u0003\u0005\n" +
                "\u0007\u0006\u0007\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "ÿÀ\u0011\b\u0001\u0095\u0001\u000E\u0003\u0001\"\u0002\u0011\u0001\u0003\u0011\u0001ÿÄ\u001F\u0001\u0005\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0003\u0004\u0005\u0006\u0007\b\t\n" +
                "\u000BÿÄµ\u0010\u0002\u0001\u0003\u0003\u0002\u0004\u0003\u0005\u0005\u0004\u0004\u0001}\u0001\u0002\u0003\u0004\u0011\u0005\u0012!1A\u0006\u0013Qa\u0007\"q\u00142\u0081\u0091¡\b#B±Á\u0015RÑð$3br\u0082\t\n" +
                "\u0016\u0017\u0018\u0019\u001A%&'()*456789:CDEFGHIJSTUVWXYZcdefghijstuvwxyz\u0083\u0084\u0085\u0086\u0087\u0088\u0089\u008A\u0092\u0093\u0094\u0095\u0096\u0097\u0098\u0099\u009A¢£¤¥¦§¨©ª²³´µ¶·¸¹ºÂÃÄÅÆÇÈÉÊÒÓÔÕÖ×ØÙÚáâãäåæçèéêñòóôõö÷øùúÿÄ\u001F\u0001\u0003\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0003\u0004\u0005\u0006\u0007\b\t\n" +
                "\u000BÿÄµ\u0011\u0002\u0001\u0002\u0004\u0004\u0003\u0004\u0007\u0005\u0004\u0004\u0001\u0002w\u0001\u0002\u0003\u0011\u0004\u0005!1\u0006\u0012AQ\u0007aq\u0013\"2\u0081\b\u0014B\u0091¡±Á\t#3Rð\u0015brÑ\n" +
                "\u0016$4á%ñ\u0017\u0018\u0019\u001A&'()*56789:CDEFGHIJSTUVWXYZcdefghijstuvwxyz\u0082\u0083\u0084\u0085\u0086\u0087\u0088\u0089\u008A\u0092\u0093\u0094\u0095\u0096\u0097\u0098\u0099\u009A¢£¤¥¦§¨©ª²³´µ¶·¸¹ºÂÃÄÅÆÇÈÉÊÒÓÔÕÖ×ØÙÚâãäåæçèéêòóôõö÷øùúÿÚ\f\u0003\u0001\u0002\u0011\u0003\u0011?ó´B¿+0\u001C`\u0003JÈ\u00ADÆÎ}=)J\u0005\u0090\u0010\u0099$|Ê[¥$¾\\l\u0001'\u0091\u0093Ï\"¿\u00AD\u0012mê~$5ãTb\u0004\u0085¹íKå\u0095R\u008A¹8â\u009EcHþu#\u0007¶y¥fyW\u00051\u008Eô¹®2\t\u0011\u0095w6x\u001D3Þ\u0095ã\u000E6\u0084*r\n" +
                "õ§à\u001E\n" +
                "1õ\u0003µ5\u0003«c$ýO5i+h\u0003\u0091z\u0090qï\u009EôÖP¨cØO\\\u009A_,\u0084Ú§<÷\u0014äÜ«\u0099\u001Bð4\u0095\u0098\u0011¾]@ËeG@hÀS\u0098Ø\u009EG'¥HQ\u0013lÊ9Ç\u0014!.\u0099F\u0007\u0004ã#\u0014s&\u0080\u008EE.\u0003:\u009C\u001Càf\u0080\u008CT+r;dô§¶s¹Fxè=i_\u000E2y=8£\u0099E\u0001\u0012Æ¿ê°\bã+Kó¬\u008606ã\u0092\u0001©\n" +
                "°Â¨è?*i\u008DN}ÏZWMê\u0003KF>P2s\u0093\u008A,\u0002ì<\u009Cç\u0014àU\\\u0012¤ñ\u009C\u0081O\u008D\u0083\u0080Û01Ò\u0087¦âz\fWÚ\n" +
                "\u0084Ú\u000F§õ Ç\u0011llÉ8Ï8§.\u0006UÐ\u0003\u009EO\u00AD\n" +
                "C|§<\fd\u008A,¬1±Ä\tÃ\u008EøRM\f>r¨vóÎ\u0007ZPªÄ\"©\u0019\u0019Îx§\u0018\u0082\u0012±±%º\u0010i=õ`0\u0017\u0007\fÆ\u008CaFö'\u009Eh\u0097ç'\u0003\u0018\u001C\u001A\\H\u0014\u0007b[\u001Ft\u000EµMXB0Ú\u0085ñÏ½1\u0002.aUÆFxã\u0015!l6N\tnÞôÝÛÛiaÓ\u001CÐ®Ö¡~â\u0018¢E\u001B\u0097qôÍ\"©Ù\u0096<tÇjq\u008E(ÇÏÁ\u0007¨4¡\u001Fq\bÙôÍU\u0093@\u009DÆÈ\u008B\u0080Û°\u0014u\u0007¥7\n" +
                "Äñ\u009E88üjA\u000ES\u0001~¸8\u0006\u0088\u0015\u0090|ËÓ¡\u001Dé-\u0085¢W#12s\u009CñNDfÁ\u0001\u009C\u008E¢¤\n" +
                "ÅÎb\u0004ç\u0095'¥+Çòo\u008C\u0010Aä\u0093I;éÔ.E\u0018Û\u009F9N;\u0003ëD\u0081ÂÈ\t?Ê\u009C¨^3\u0090p\u000F\u0006\u0094\u0086Y6.0À|ÄsT÷\u000B»\u0091ì\n" +
                "\u007Fx½¹\n" +
                "zVï\u0081õ\u009D\u001FN\u009EâÓ_Ó\u001AîÒEW\u0016ç\u0018Y\u0007FÁã8,?\u001AÇx\bRÍÈ4¨ªFåB\u0001=ÍsbðÔ1´%Fª¼eº*\u0015%N\\ÑÜE*8a\u009CÐ\u0017kä/ÊG$ö©DqîÌ\u0080ç¶;RE\u0080ÌJ·=9\u00AD\u0093m\\CK¬ªpz\u009Cýhd\u0004|ÌN;\n" +
                "v\f\u007F3)\u0007±-Ò\u0082 ®[\u0091\u008CõéIZ/È-b5\u0001\u0089\u0091Ë\u0080;\u000Eô\u008C»ÀÆT\u0093Æz\u0081R\u0088÷à\u0011Ç_»B A\u0097lg¡ÛÅ4ô¸ï¨ÕR\u0099!\u0081Ïb)¤«\u0011ÁãÓµH\u0002\u0083Ûv4\u0085\u0004D2~\u0004u¤\u0098\fo0¹gB;c=hRvme#iåqÒ¤ \u0096\u0093×\u0082hp²\u000FÞ\u0012\u008Cz`PåÐ\u00066YG¡\u001C\u001FNiQ\u0014ÌX\u008EôëR\"\u0018Ø(BF>`xÅ\u000EM¹2+dz\u009EÔ\u0093¾\u0084¦Ù\u000B®X²Èsß=¨Ê2\u0095f8ÎzT\u0091\u0085Ý\u009D\u0099î[Ö\u0095\u00179PØôã9ô¦¤\u0090î\u0086(\u0003/³\u001Bz\u001CÓWiÊ\u0086#ß\u0014à\u001A5\n" +
                "Ä\u0013üDô\u0015\"¢\u0010X®3Æ\u0007\u00AD&\n" +
                "\u0091 \u000B\u0099\u0011wsò\u0093JÁ¸]¤ääóúÓÊ\u0012FÞ=ÏzIRt8\u0003\u0019ìOJKVJÔdûW\u0093\u0093¹yÀéHÑáSËíÐúÓ\u008AÍ!Ëã\u0091\u009C\u0083Ö¤c\u0086\b\u0006}2zSNÈ{\"  \u0092¤qÒ\u0090¡Dó@Ç=IëR\u0011\u009C\u00120Üþ4Ø\u009Brb`Ã\u009E\u0086\u009Aw\u0015Ø\u0085UÊ\u009Cd\u008FLSv\u0085Îá¸\u0013À\u001DªF9bUxÜ03JH\u007F\u0093\u001B@\u0004\u0091Þ\u0095ßÈ:\n" +
                "\u001F\u0097iÚ}©6î\n" +
                "\u0002\u001C\u0083\u008Eiâ!\u008C\u0082pz{ÐÊì\u0098È'<\u0085\u001C\u008Aw´·\u0005 ÕmÙ\u0018>Ù¤\u0005À&D?59V@§åã=éÅC\u0001µÏ\u001EÝiì\f\u0087Ë%¼À\b+Oh÷¾6\u008Cõ8©\u0019C1-\u009C\u0081ó\u0001ØST\u0083ò\u0085Ü¤cq84Ö¢»\u001B\u0080¼m<\u009E\u0082\u009ACD\u000B2nÉèOJ\u0095\u0090\u0097ç \u0003ôâ\u0094\u0081\u009D«\u0092XôÍ-\u0013\u00041\u0080)É#$\u001F¥$ªV5R\u0003ã¹©\u001E-¹F9$þT£,\u0081\u0002\u0085=r}(Mn\u0003P±LñÀÁÇ\u0019¥\u0003'+\u001F\u0003ÐÒÅ\u001Aá\\\u008E\u0099\u0005OZF\u008DØù íõç\u00157±Iê7iS\u0095`\u000ErAç46v\u0090X\u0096Ï÷iB\u0005\u007F\u00939Ý÷\u0088êiYÐ0!~÷ÞÈéNé½Jê#GÈP\u000FÐÐQXòO8ÆG\u007FJs£î\u001D\u0001ÎA4»\u000B\u0006w$ú:ÒÖÄ«\u00ADÆG\n" +
                "¾à²c\u008C\u009CÑÂ®æ#åä\u001CR¬J_ÍBq\u009Cb\u0089\u000Bï\u0001ù\u001DG·Ö\u008E»\u008Fq¬\u0099]îÜvÇ\u0006\u0095\u0095¢a´ä·ROJy\n" +
                "\u0014\u0003ÉîqMt*w\u0019\b\u0004ÿ\u0010¤µ\u0006' \u0096|ð3\u0093H\u0011Èê0q\u0080j@ª\u0013ü¹ç\u001Cæ\u0082\u008AÌs\u00921Å\u0017Z\u0086\u0088aUR0\u000Eq\u009E;Ñ\u0019R»A ç¡©3´m ¯nE\"ÆÄlgÝêqJátÄf*¼(\u00188\u001DéQ\u0098¢õ\u0004\u001Ep)ûQþR¿@\u00054'\u0096Áw\u0015'¦yÍ+«\u0093¥\u0083Ê'æ\t×½0²1Â)$\u000E\tïR2¼\u0099;È÷\u001D) Vd.ã' ã\u0019§t\u0095ÁZÃQ\u0087j`{ÿJ\u0010\u0001ü[·\f\u0002GJz.Ü\u0080¸ô\u001E\u0094ÒÉ»Ôö\u0006\u008B6!2\u0019'#¨Å5âI$\n" +
                "»\u001Bz\u0093Þ¦\u009BqPÍÉì=)¢5\u0018\u0018ÎGSÚ\u009E©Ü\u0006l\u008E1¶ROû´(\\y\u0081\u000EO\u0003=iËÄ\u009B\u0088à/ \u007F:PÒ²îF\u00041ôæ\u0086ì\u0003~c\u008C\u001C\u008Cò=iIc÷Tqü8¥Çe=>öhÝæ %\u0088÷öªV°\bû\u0089\u0005\u008EqH£\u009C6sÆÚ\u0099\n" +
                "2íÀÆz\u008FëM cx-×¿jWI\u0080Â§\u0003ä#=Hô¤\bñ\u00903î3R ãvHÁîiFWæ\n" +
                "MSq\u008A\u0001¬¥\u0086N}A¤U#\u000EbÁ\u0007\u00ADKå¸<\u0092A?ÃAù\u001F\u0095À==i^û\u0089Ü\u008D\u0015U\u008EÕÆ{\u009A<½ÇÌ\u001C\u0083Ç+R\u0086s\u0090Ààq\u0082)T\u0004\u00043c\u009EFhOQ\u0090à\u0001ó\u0003ô=¨e,B©ÈìiÛU\u008B\u0013\u0019äôcK°î\u0005_\u000E\u0095\u001DGr3´°F\u0007\u008E¸4å@\u0006ä\u001Cç½.mìã%½)ûr¾_SëéEÁ¶È\u0093Ën\u0001ù³Ë\u001FZqM¸ùóÏÌ\n" +
                "+\u0004V*Nqß¹¥Ú«\u0087\u0003<~T\u009Eâ\u0013fì\u0080x\u0007ó¨×\u0018%É8ëÇZ\u0094\u0010¡\u008E\b>\u0080ô¤hÛ!Täÿv\u008D,4®!Ä¤à\u0010\u000FCC\u0080@ÝÏ\u0018ÛëK\u0082\u009C\u0015\u001Ctæ\u009E\u008AÀd6GRH¥µ\u0098jD\u0088\u008ALa°:©¥tBÀ¨$c\u0004\u009FÖ\u009F\u009C\u0091¿$ã\u0090(ò\u0082¡'\u0093\u008EFhµ÷a¹\u001A\u0085'%\u0089ÇAJÄ+d\u0082Ý\u0089Í8\"¢\u0085ã$ôÅ;\fxÈÎz\u0091\u0081GMA\u00ADH\u0098\u0016à.ÓÚ\u0094©_\u0094>q×&\u009Eñ\u008F$3\u0002\b<úu Â\u0007ÞA×<{ÒRAq\u008A®\u0013ýaã¡\u00141\u0099T\u0001 \u0018\u001D=)ÿ(\\© ÿZyfeÉ|\u0093Ôâ\u008Bù\u0003Ñ\u0091Äï\u001DqÉ<æ\u0090¦àdc\u009Cuâ¤^\u0081VN}M8mQµÀàó\u009A/Ñ\b\u00897\u0091\u0081ÎAÀþ´\u0082)\u000B\u0082§wn½j@Çx`ØÇ¥+m\n" +
                "\u0094\u001B¹ÀùºSÜZÜ\u0087\u0003{\u001D¼\u0003ÐSÁR\bA\u0082{Ô\u0084+/-´c°Îi\u00020Ë\u0013\u00901×µSiî\u0017\u0018±\u0092J4\u0084\u0081÷H\u0014«\u001FE# ê\u00058\u001D£\n" +
                "p=1@%WkJ9\u001DE+\fg\fJ\u008E\u0083®\u000FZVFÙ÷\u0089Ïj_/'\u0004qïOe\u001DÏlñTß@#U\fªÌ 1ê\u0001£,£~üç±\u0018§`oÝ»è{\u008AvX\u000E\u0018\u001Cw=(°\n" +
                "ÇÊ\u0014\u00102{\u009AvÀr®Ù\u0003§\u0014à \u0090á\u0083\u0016\u001Cãµ\u0011®\u0013#>\u0084â\u0087Ê£tMØÀ\tÎOONiL^n\t`xå\u0088§í,¼9\u0007é@P\u0006âz\u009E\u0094ÚÐIêBd@»@<\u007F\u000E:SYTâ2F3ùSÀ\u008C(v\\\u001CñF\u0010\f*\u009E¼\u0093Þ£DXÖÃ\u009D\u0098Á\u0007\u008CúPCv\u0018$â\u009F±0\t\u0003\u009E¼ô \u0083ÂñÏA\u009Cþ4ôè;²\u0014VI2\u001BóíOÚÅYq\u0091\u009E¢\u009Ec\fÅs\u0092Os\u008Ath£\u0007\u00198ïÐR\u0093BÜ\u008D\u0011s¸ä\u0083Ô\u0081K\u001C{@A×±\u0002¥e\u0088>TüÙç\u009EÔ\"\u0095'\fN\u0007j\u008B¶\u0080\u0085\u0089cå¨Á=M.ÖfØ¨FGÍï[Þ\u0004Ò|\u001D©ø\u0086(<}â\u000B\u008D3MaûÛ»KO=Áì\u0002ä~uìß\u0010?aÍZ\u001F\u0086\u0011|jø\u001Fâäñ\u007F\u0087\u009E&\u0092Q\n" +
                "¹\u008Eæ\u0005\u001Dw&NHî\u0005|o\u0011x\u0081Ã<'\u0098á°y½WGë\n" +
                "F\u0013\u0094$©9½ êÛ\u00922vÑI«\u009E\u009E\u0013*ÇcèJ®\u001D)rêÒk\u009AÝùw±óìhb\u00078ë\u009A\u0011D¯·\u00808ëR22¹F\u008C\u0083\u009EF:\u001FCRZ[\u001B\u008B ©\u0011`\b\n" +
                "£©>\u0095õÜéEÊúXóâ\u009Dìuÿ\u000B¿gï\u008B?\u0018üé¾\u001Cx*ïQ\u0086Õ\u0082Ï4QüªHàdñ\u009AæüOá/\u0010ø3^¸ðß\u008A´¹l¯md)=¬é\u0086F\u001E ×êÇìoð\u0092/\u0083ÿ\u0002´\u000F\u000EÍ\u0002\u00ADåÅ§Ûµ\u0017\t\u0082f\u0097\u0007\u001Fð\u0010@®3ö²ý\u008Eþ\u001B|hñzøßRºk\u000BË\u008B%±{\u0088\u0013\u0091)uÙ1\u0019ù\u008A®W\u001Dóí_Á¹\u007FÓK\u0003KÄ|^_\u0099áÒÊã)Â\u009DJiÊ¥âì¥$Ý¥\u0019Ù´\u0092M]o©ú\u0085O\u000EªTÊá:3ýóIÉ=µè´½Ñù\u0092Ùc\u0083\u008Ez\u009EÆ\u0091\u0019\u0083r=\u0094\u0013\u008AôoÚ7övñ\u008Fìíã·ð\u008F\u0089q,\u0012\u0083&\u009F¨F\u009F»¹\u008FûÃßÔv¯>\u0010`\u0094ãÔf¿·x{\u00882~(Éèæ\u0099]eV\u0085hóBKf¿4ÓÑ§f\u009Ai«\u009F\u009Aâ°\u0098\u008C\u0016\"T+Ç\u0096Qvi\u0091\u0011\u0085 \u008EO\u001FJ\u0018e\u0082±\u0004\u0083ò\u0091éZz'\u0086u¿\u0010Ü}\u0097CÒ./$êRÚ\u0012Ç\u001F\u0085G«è\u001A\u009E\u0083zÚv±a5µÄ\u007F~\tã*Ãð5Ú³\f\u000BÆ<*«\u001Fkkòs.kwå½íçc9R«Éí\u001C_/{i÷\u0099ì\b\u0005Q\u0083é\u009CÒ\u0095gÃ\u001C\u0002½jV\u008A v\u008C\u0093\u008EF)¾X_\u0099F}\u008DvÇS.\u0084\u007F}¶ì\u0003\u009EN)B\u0090`=¸ïN\u0003\bpÀóÐ\u008AR¯\u0080\u000E0O#\u00AD5d\u0004m\u001Bl\u0007°Ï#¥\u001F&7\u0012O\u001C\u0093Ú\u009E#Ê\u001Dù#9\u0018¥g@¹À'\u0018§¨\u0011ª\u0094\u0005¶\u009D¸ü\u0085)U#\u0004sÛÚ\u009F´*í\u0098\u0086ã¡=(B6|¬½yîqNÄÜ\u008CDÙ>g\u0019\u001D\u000FøÓ\u008AnPÀc·\u0002\u009CÈÅ²[#\u0003\u0014F\u0018\u0015\n" +
                "p0@ö¦¶\u0006ô\u0018P»`\u0011\u0095ç¥9\u008EF[o¹ÇJP!sÉèÆ\u0094mêê=[=ê¯ ®ÄX\u008C£i=¸íF\u0018ýÖÁíÍ9p§j\u0091Ö\u009F\u0080N\u001C\f\u0093IY·aõ»\"<\n" +
                "¹9ëÅ\n" +
                "\u0018\f)9õ=*]\u008Bæ`\u000E¾¾\u0094¦1\u0092ª\u000E)ót!´T\u0007æÁ\u001Cuù}h@¤22t\u001C\u0010ÔèÀ\u0004¶FìpqÖ\u008F\u0098\n" +
                "Ä\u008CôÈ\u0015\t§¹¨\u0087Ì\u0018\u0019\u001C\u009EI\u0014\u008CÍ\u008F\u0090 Çñw4øñ\u009F\u0098\u0091\u009E\u0099\u0014¨\u008AÀ\u008Cuèii`\u001B\u0082\u0014â0~^M*ãÌ\u000Fêpié´1SÎzcÒ§\u008EÖB|Èáb¹áÀ$\n" +
                "Îu!\u000F\u008Bo1Å]\u009D\u0005ïÀÿ\u008B6\u009AM¶»sðûSK+¸V[{\u0095´fVCÑ²=k¿Ôÿa\u008F\u008F\u001A_\u0080\u0013Ç\u008Báø§\u001EP\u0096m2ÞM×qÆ@!\u008A\u000E{ôê+Õ\u007Fà\u0099ÿ´\u0006¡£üD\u009Bá?\u008D|A%Í\u0086±n£MKÙwªN\u0083\u0084\u001Bº\u0002¹\u007F³_]üQø[á¿\u0010]ÿkZ\u009DGF»\tÆ\u00AD£Ý²¼g±hþë¯á_Â¾0ý#¼EðÓ\u008Eá\u0090×ÃÑ§I5QUQ\u009CÕZRÙr¹'\u0016\u009Aq\u0093\u008Bnêé[Gú\u0097\u000Eð\u0086I\u009Cå¯\u0012¥&Úµ®\u0093\u008C\u0096û-|\u008FÉ\u000B\u009B\u000BÛ\u0019äµ¼·d\u0092&ÃG ÚÀ÷\u0004\u001E\u0095ôÿü\u0013oö\u0084\u0093Â>$¹ø;®_/Øµ\u00962X$\u008Dò¤á~eç¦áú\u008AöÏ\u0088\u007F²¾\u009FãÏ\u000EêÖ¾9Ð4\u008DcT\u0016\u0092Iáï\u0015Ù§Ù\u009Aiö\u009D\u0091Ý*\u007Fµ·-Üf¾M\u007F\u0082\u009FµO\u0087í4\u009DwÄ_\bt;=gÂê\u0082úóÁ2¼ÑÎC\u0016IdC\u0092\u000E8Èà×³\u008Cñ\u0097Ã_\u001E8[\u0013Ây\u009B\u008E\u001EµhÚ2\u0094ãìÕD¹©Î\u0012\u0097,´\u0092³R\u008Cd¯Ê÷\u0016\u001F\u0085s®\u0018Å,Ê\u0083ç\u0084\u001A¼R|Î-Ù¦\u0095ÖÞo¹ß~ß\u001F³>\u0095àýN_\u008C\u001F\u000FlV-2ò\u007Fø\u009AØÂ¸[i\u009B£¨\u001D\u0015\u008Fà\n" +
                "xÿìÛá\u0015ñ¿Ç_\n" +
                "xbHUã¹ÖmÄÈÜåC\u0006 þ×ØÖ\u009E*°ø\u00ADðæ'ñ\u0005\u0096ûMfÄ%Ý´\u0083\u0098ß\u0018tÁèC\u0003^Ið\u0013à\u0016©ðoöÍð\u0085¼ì×:eíÄ·:Mè\u001CH\u00827ùO£)à\u008Aù\u000F\f|iÇá|3ÎxO>\u00ADlÃ\u0005\u0087Ä*2\u0093Öq\u00859{\u0097ë:mi\u00ADå\u000BZü\u00AD\u009E\u008E\u007FÂ\u0090YÖ\u001B0ÂÆôjN\u001CÉtm\u00ADmÑK¯gê~\u0087\u0083\u0014QI\f (\u008A\f(\u001D¸¯6ø±\u00ADG/\u0087wy¡H\u001EhoB0\u0001üë²\u008BP\u0006æéY²|¤<þ5â\u001F\u0017üUogáiÚîp\u0016\u001BM\u0083æî&Çô¯ó·*ÃÊ®)z¯ÄýN\u0085'9ès¿ðQo\u0003ÚxßàhÖÒØ=Þ\u0089¶æ\u0019ù\u0082\u001C\u0007\u0019ô#ùWÄ\u001F\u0006>\u0010ø\u0093ãW\u008Fìü\n" +
                "á¸3$í\u0099î\u001BîA\u0012òò· \u0003\u009Aû\u0007ã\u007FÇ\u001BM\u007FF\u009BÁ1ª¼w¶/\f¤\u0090@é\u008FÒ¢ÿ\u0082vü7Ð|/àË½~ã\n" +
                "«k\u0017\u0005g\u0004|Öö1\u009E\u0007·\u0098ß ¯í/\u0007ücÆxWà¾mBZâ!R?VO[J²\u0092nßË\u0007\u00076¶mÛí\u001F\u0001Ä¼(ólû\n" +
                "U¯q§Ïé\u001B[ï½\u008Fqø\u0001û5|<ø_àø4\u001F\u000Fè±üñ\u0086º¿\u009A1çN\u0007WcÔdô^\u0080W\u0089ÿÁTü\u000BðóDøy¢k\u009AO\u0087 \u0083X¸ÕD\u0006ò(þi!XÉ(ÍÜä®>\u0095õ¤÷Í§ÚÛÚ\u0015Ty0fÁèÎ?\u0001ÅyßÅß\u0002Xüb½Ðü+¨iP^*ÜIxÍp\u009BÒ\u0011\u008D»±ô$Wó\u008F\u0003ø\u008D\u009Bä¾&a¸³2«R´¡QÔ©ïµ*\u008B[ÅËUg³Vµ´±ôxü¢\u008E/(\u009E\n" +
                "\u009AQ\u008B\u008D\u0096\u009A/;\u001F\u0094³Û\u0095@¬pGL\u008EÕ\u0013ÄTãçÖ¾òøÏð_áÏ\u008F<}\f\u0096_\n" +
                "mÍµ\u008CÉgce§ÚùMvü\u00034ì¿ÃýÔ\u0003$rO4ÏÚ¿àÇÂ¯\u0084\u009F³åî³\u007Fà}\u000E\u000Bñ\bµÓ¶Ûí\u0090ÝIÐ)\u0007øFXç=+ý\u000FÊ>\u0097Ü9\u009Aãòì»\u000F\u0096Õ\u0096#\u0015(ÁÆ2\u008BPri$\u009B³\u0095®\u009BÑ%ßF~W_Ã¬n\u001A\u0085ZÓ¯\u0015\b&ÓiÝ¥å\u00AD\u008F\u0083Ú0\u000F\u0098G=0E#\u0080¬\u000B\u000EjR<´ ©lg&\u009Bå\u0087Ú\u000B\u0013ÎFkû\u00054ÙùÀÝªÄåûóô¤uQòt\u0019ç\u001E\u0094ä\\`8ÎM9@]Å\u0086Oz¦&Ñ\u0019\u0085I2#d\fb\u0080\u0001|ÄC}E=#b\b\u0007\u0004\u009CÑ°\u008EDxöÍR\u0093Ø\u009D\u0086®òJ°\u0003\u009FÊ\u0094Ã\u0090@ äòsÍ;ËNxÁ#\u0093H©Ë\f\u0081\u008E\u0082\u009Aµ\u0081·qÎ;óÎG4¾Q*Hî:úR¬-´¶{ýÞô\u0089¿\u008EF3È\u0006ªý\u0010¯ÜrG\u0018ÚÃ&\u0091WÌù@#\u001E§\u00AD)E\\qÀíOX\u0094\u009E½³Ö\u0092J=A´1Bq\u0093\u009E2qÚ\u0094õå¸\u001E\u0095&Æ'\u0083\u0091\u009AT ®J\n" +
                ".\u009B ¦\u0002\u008F\u0095\u0014z-+\"ÊøÁû¼Ò²³1f^yëÚ\u0085àn\u0003¦Íg{êÎ\u0081\u008C¿?Êz\u000E\n" +
                ">\"À\u001C{ýM.Ã¼\u0087À\u0007¾(x\u009Bïc\u001E ~´¤Ð\u000B\u001AáC°çÛùWÒ\u007F³ßí\u008Bà\u001F\u000B]iþ\u0001ø\u0087ðOÃ¿ð\u008DH«\n" +
                "ÔñY\u0087\u00998Á\u0095\u0099\u0086[\u009EO5ó\u008D½¤óÿÇª3\u0093ÇÊ¹Ç¿\u00153[¼\u0012y2¯Í\u008EAà\u008Aø\u009E6à®\u001AãÜ±å¹¼\u001C\u0092¿/,å\u0019BMYN<\u00AD{Ë£w±ée¹\u009E7)\u00ADípî×Þé4×g~\u009E\u0087èÖ\u00ADû\u0006~Ï\u009F\u0010 ·ø\u008Fðo[\u009FDº\u009AE¼Ó¯´Ù¼È\u0003g \u0085ì?\u001A÷\u001F\u000E\u007FmGáè4\u007F\u0019\\ÛÍªC\u0002\u008B\u0099í³¶R8Þ3ÈÎ2Gc\u009Aüêý\u0090?l_\u0012|\u0006Öbð¯\u0088¯%»ð½Üø\u009A\u0016l\u009BV<oONz\u008FJûÎÛâ.\u0091\u00ADi\u0096þ Ò/\"¸\u0082hD°Í\u001B\u0082\n" +
                "\u0091\u009E\býE\u007F\u0094\u009FH.\bñ/\u0082³:9V}\u008B\u009E3\u0005\u0017'\u0085¯?yò»^\u000EoÞRI+ÁÉ¯µ\u001DÏÝxO0Ê3J\u0012\u00AD\u0085\u0082\u0084Ý¹â´×½¶·\u009FÞr\u009F\u0016VÿÁò\u001DRÕ\u009DmK\u0083,hNÔ?ÞúW\u0098_üP¿ð·\u0088bø\u0081áë\u00ADª0·A\u001B!×=ýkÚ¼C©é^1ðô±NªUÑ\u0094®s\u0083è}«äÿ\u0019]·\u0080uûÏ\u0003ëDù.Yí\u0018\u008E\fly\u001FQ_\u0091dta\u008A\u0083§Q{ËuÝuû\u008FÐ°t½´\\ZÔ÷?\u008A>-ðFµáH<[á¨\u00AD\"\u0096í7ÝZÅ\u0085ÜHûø\u001DóÞ¸\u007F\u0084\u009E+}{Ç~\u001E°\u009EhÙ´\u00ADhO\u0003¹ù\u0095Y\u0019H\u0007ß85ó>µñ/Wð\u0087\u0088N\u0091%ãí·\u007F6Ð3ü¯\u0011<\u008Aê->#Ëàÿ\u0014i\u009E4Ó\u001B6ó\u0015\u0095\b<\u001Fâ\u0003óÈ¯¨§\u0090VÂáÜa+¹'Êúê\u00ADo¹´w,±{\u001E_¸ûÎßÄË/\u0089®ì¼Ì\u000F°\u0092F\u007F¸Ø¯\u0092\u007Fko\u001DN\u0004:6\u0093~\u0004R\\Î.\u0002·<K¸~\u0015è\u001A\u0017í\u0005áï\u0012x\u0083IÖôkåÛ«ÛÜG4f@\f'fò\u000Fâ¤WÏ?\u0018|AmwáÛ\u009F\u0017»y\u008D%ýÅ¼Y<'ÌrkËáì¦x|zu#ÙZÝu_¡\u0019v\n" +
                "K\u0013i\"\u0096\u0087¯Ëy\u00ADÅqu'\u0098w\u0082Û\u008ErF8ü«é\u000F\u0080\u009E2Ó´¿\u0089\u0096Z\u008CrC\u000E\u009D¯G¾[xHÛ\tC\u0080\u0098íó\u0001ù×Çþ\u0018ñ\u0012+E2\u0091\u0093\u0096Î}ÇøW¨ü\"ñ¼vÚ}Í¼¸óZd\u0096\tsÊ2\u0093Àö9þUôùÖ\\êáå\u001E\u0096·ßþOSLN\u0002ðKÔû£_ñ\f×\u009A\u0085á\u0085\u008B\u0018Qmâ\u0003¼Ò\u001E\u009F\u0080Çç]lZt^\u001EÒ%¾@¿jke\u0081$aÐ\u0001þ9?\u0085yWÀ-M|xútÍ!\u0094[3]ê.GYÉÂ)ü9ü+¸øÉâ\u0089¬lS@Ó\u0018µÕôékj\u009D÷¹Á?\u0080æ¿\u001FÄP\u009C1QÃ-Öÿ×ÊçÏÔ¦ã5Oï*x\u0012ßE\u0085\u001FÄ\u0086\u0005\u0010Ç3Çg#/2¾~yO©Ï\u0015ñ/ü\u0014£ã\u0093üEø\u00AD\u0017ÃÍ2ãþ%\u009E\u001BC\u001B\"\u001E\u001EåÆ]¿\u0001\u0085üëì=S^´Ò#¸Óìä\u001FcÐíü\u0095$ðî\u008B¹ÛÜî#õ¯Ì/\u0088\u0017\u0097Ú¯\u008Dµ\u008BíUË\\Ï¨Ë$\u0084õ$¹=\u007F*þÀú\u001Bp\u009E\u0003:ñ.¾m\u008B³xJNTÓþi¾E/ûv-üä\u008FÏ<FÆÖÃd±§\u000Fùy+7ä\u0095íóÓî0\u0084{\u0014å\u0081ÀÁÏZC\n" +
                "\u0080\tb3È«\u0016¶3\\Ê ¶\u008DäwáQ\u0017%\u008F õ5èÞ\n" +
                "ý\u0093þ6øÒÞ;\u009B\u001F\bI\u0004N¹\u008Dï\u009BËÈö\u0007\u009AÿL3þ,á®\u0015Ã,Fq\u008C§\u0087\u0083ÙÔ\u009Cc\u007FK»¿\u0092gâ¸<\u0006;\u001F7\f=9Mù+\u009Eb\u0013'{\f÷\u0004\u009E\u0094È÷°%W#Û¸®çâ\u007FÀ\u007F\u0088_\n" +
                "Qn¼S§Göw\u0098Åö«i|ÈüÎ»7\u000E\u0087\u001D«\u008CÃFw/AÚº²\u000E\"É8£.\u008E?)ÄB½\u0019])Á¦®·^«¶æ8Ì\u001E/\u0001[Øâ á%Ñ¢%VVÆ9>´\u0085A\u0006GR\bè\u00079©1\u0011Ë\u0091\u0096'¦zR¬jf\u0007ñÅ{g)\u0019]Ü\u0086g·4ä@ÙUôïO\bW'\u008Cu¤\bìä\u009EwtÇ\u0018¡k¨\u0093\u001A°£\u0002À\u0016#¨\u001D\u0005(\u008D\u009F\u0001Pp8§\"º\u008D\u008A¥@ëïN\n" +
                "\bÞ®\u0001>\u009CS»L}5\u0018\u0017rîÇ4¡NÒYF{\u001CÓ¼£\u009E \u008E\u0099¥*Ê0\u0017\u009Eäw¡«¡]\n" +
                "\u008C.yÀÇS\u008AY\u0001À\u009FÂ\u0095P\u0007?)ÀíJ@\u0003\u0095ïëMovISjª\u0082\u0013\u0007\u001Cs\u0093Fea´ýE8ÆÛ~EÁ\u001D\n" +
                "\f¤`mÇ\u0019 ÔèÍ®1\u0095\u000F\fÄg\u0080sN\u008C\n" +
                "ÙÜÄ\u0080r=iÊ\u0006\u0003±\u0018ÎO¸§ \n" +
                " \u001F0ôÀíQ'\u0014\u009BèR»=\u009Fö]ý¦4O\u0081K.\u0099\u007Fð¯JÕRú}×7÷P+Ì\u009Ca@Ü\b\n" +
                "=\u0007\\×ÔW~\u0014ý\u009Aþ?è««x\u009FáU\u00942O\u0018'RÑ\u0088\u008AXØú\u0085ãô¯4ý\u0093ÿà\u009EÖ~#²_\u001E|pÔE½¬J%\u001A\u0005¬\u009B§dÀ!\u009F\u001C >\u009Dkèß\u001AþÏÚU®\u009Bkâß\u0082öÿaX-\u0082Ic\u001A\u0090$\u008Dz0\n" +
                "ßÔ\u001Aÿ->\u0091ÜqÀrãÈâøC\u0017V\u009E>-ªõéT\u009CiÉÙr¥.mZ·Ø\\\u008Dw?xà\u009C¿\u001Dý\u009Aðù\u00AD8ºnÜ\u008AI]zé³óÔù/ãOü\u0013óÄÚ\u0005¼þ(ø+¬§\u0088ôÅRígÂÝÄ?Ýþ<{sX\u009F³/Ç\u008F\u0013ø\u0002êo\u0085Þ%\u0095ã¶vÿFK°U\u00AD¤Ï+ÏAí_Né~ ÔôÙót\u0092[ÜFÜÉ\u0016@${v>Õ\u0087ñCà÷Â\u007F\u008EÃÏñ5°ÒuÜm\u0083]±P¥\u009B·\u0098£\u0087úõæ¸òï\u001Fëqg\u000BÕáo\u0010iýf\u0084×¹\u0089\u008CWµ¥5ðNQZM'»\u008D¤ÕÓæ¹Ý_\u0082^W\u0098G\u001F\u0094>Y-àß»%Õyytô)\u000F\u008Brø:ñuI\"fÓ$\u0095WQ\u0089N|\u0096=\u001D\u007FÙ5É~Ö\u001A6\u0095ã\u000F\u00037\u008Aü=:M5ºùöSÄßy1È¯<ñ\u0095·Å?\u0081ZÓx\u000BâÅ\u0093Üióf=\u0013Ä\u0090©{KØOH\u009D¿\u0082AÛwµy~±ñ«Tð\u0016£/\u0082µ½Eÿ²n\u0098µ¤\u008CÜG\u009Fä;\u001Aü\u0083\u000FÃü¸ÅW\t5>]¥\u001Dc8÷]~O^\u008D\\ý3(O\u0010£Q{²[§ù\u001Co\u008C5óã\u00AD\u0012\u001DZÂä®£¥¶d\u008F»¦pËøu®÷áà\u008BÇ\u007F\f¯|4'\u0006òÊ/?Olä\u0091Ô\u000FÌcñ¯\u009Dþ.øÑþ\u001BøÐx\u008EÉ\u008FöuÛþý\u0013 ÉäÿZ½¡þÑiðÞxüC§]\u0007´\u00963Ñ¾ôn0\u007F#Ïá_ VÊ1\u0018\u008C$=\u0082ó\u008F\u0093ê¿®çÚû\bN\n" +
                "-\u001Eæö\u008Bñ\u0097Zðþ£~,ïäK\u009D&è¹Bßw#?â+¡·øÅmã\u009F\u0080\u001A\u008Dè¼O6Ï_mãw#Ì\u008Fwô5ñÿ\u008B>0Í£ü[Ôe\u0096ô½¶³\u0003\"H[\u0082Ç%\u000Fã\u009CSþ\u0019|lº²ðV¿áÉç\u001EUÈYÈÏGDeþµôU¸YÊ\u0094k(ëx¿óýNXVÂ¼RWWNÏå¯â}=¡øú\u001D?O7²L6Ciæ7Ïõ5Ûü\u000Eø\u00876\u00ADd\u0097Îy\u007F\u009E@[¶x¯\u008DOÅço\u0087wH.?y<ÑÛ)Ïðñ\u009FÓ5ë\u009F\u000Eþ.é¿\u000F~\u0018/\u008B5)@S\u001Fîc'\u00991÷@ú\u009AáÌø~qÃ´£y9Y\u000EN\u008D]\u0016ö¹ú\u0081û\u0010üyÑôMc]Ñ5{ØÅ½¾\u008Eú\u0085Ë3\u007F«(2\u0017ñZï¼\tñbÛÇM7ÄíJa%·\u0087í.gby\u0006åó´\u000FuOæ+òûÀ?\u001D5¿\u000Bü-]NmE\u0097Vñ|Ì.\u0015[æò\t\u001F/Ó\n" +
                "û·ö7·\u0093^ø\"¾\u001F¼\u0094´\u0097ú\u0092½Â\u0083\u0092!P\u00AD!?R\u0015\u0005~7Ä¼3K-u1rûMGä´\u0093ü×ÈùìÇ\u0001N\u009A\u0095nöûº\u009D\u008F\u008D<lþ\u0017ð\u008E\u0091g©¸\u0017ÚíðiÕ\u008E\b\u008C\u009Dò7Ósmü+å\u001FøT\u001E'øÛñßUðÇ\u0080tÝñ\u009BÇiç#\u0011[§\u0019wnÃù×¦~Ó\u009E5\u009E÷ã]å\u008FÛ\u0015×L\u0085b¶\u0085O\u0011\fp¿RNOÖ½\u0097ö(Ñ´½'á\u0095ÕÔ6k&¥s©1¸1\u008F\u009EîCó\u0002ç¨EÏÒ¿DðÓ\u008FêxC\u0097csÌ,\u0014ñU©*t¢þ\u0014ç%'9wPQI.\u00AD®\u0097>\u0007\u008A8yg\u0094hQ©¥5.i?\u0095\u0094W\u00AD÷è\u0091©ð?öAøGð3G\u0083Ä:ä\u000B\u007F©:\u008Cß\\EºY_û°Æ~î}zý+WâÿÅ\u008D;Áú[ÛÞ9±F;!Òt¶Ss!=<ÙyÙ\u009FAÍSøÏñ\u008E\u000F\u0003\u0099-l¯\u0016û^\u0099\n" +
                "´ê~KE<b1Û\u001E½I¯±×#¶×Ç\u008B|s3Ïojæêx\u009Dúíç\u001C÷5ù\u009E/\u001BÄÜu\u009BË5ÎëÏ\u0011ZoNgvï´`¶\u008A¾\u0089$¼\u0092;pyv\u0017.ÂòQ\u0082\u0084\u0017m>oüÙ¿ûZÙhþ\u000Eý\u009D!¸ñ\\\n" +
                "ºÿ\u0089îâ}+HóË\u000B\u000Bu;\u009AL\u0013\u0092ä`n>µñùÞIs\u0090\u001B\u0090\u000Fjî~:|añ\u0007Æ¯\u001CÜøÃZ\u0098ùG\u0011Ù[g\"\b\u0087ÝP;W\u0012Ä«ä7\u0015þ¯}\u001F|;Çxqáý<&=ÿ´Ö\u0093\u00ADQt\u0084¤\u0092T×øb\u0092o¬®Ïç\u009E1Î©çyÄªSø ¹bûÙïó{y\u0011y`d²\u009C\u009E\u0086\u0083\u0095\u0001H&\u009CÑ¤\u0080¸éê\n" +
                "*, òqÆFy¯Ý:]\u009F'f4í\u0018\n" +
                "\u001E0s\u009Cô¥Ë\u0004À$\u0092yãµ8ª\u0095ÚÜ\u009F_jB`¾_\u001D\u0081¢×Ø6ÜiB\u0017k'\u001E¹¥Ør\u0006ãé\u008Ap\u001B[\u0091\u009C\u000EqHCd¾x>Ý*Ò¶¬M¶)\u008EE;z©ï\u009AQ\u001Ap\bÅ;s/Êã\u008Cg\u00AD*#\u001C\u008CóEÚWb\u0013ýX\u001BrN{\u000E´\u0092\"\u0016Ë\u0001ô\"\u009Eê ù\u0085qÉ\u0002\u0097Ê.Û\u0095wdzâ\u0084\u009Eàgª\u0098\u009B\u0004±\u001CãÞ\u009C±ðw¶xÁ9íJÑîèp3Cí_\u0097nA\u001CVKsk¦\u0001U@#\u0090?\u0095,NÑ\u001Dè\u000EAýiª¸9CÍH\u008A\tÁ'§\\ÔI+j\u0087ªz\u001Dg\u0087~8|Uðß\u0088åñv\u0087ã\u00ADFßQ\u009FýuÂ\\\u009F\u009F§\u0004t#\u008E\u0098¯yø_ÿ\u0005?øÓá3\u0015§\u008Eà·×íT\u0081+È\u00829±ìW\u0083ùWË\u008A\u001E&!Ç^õ.8\n" +
                "§,rsÞ¿<â¿\n" +
                "|;ã\\?²Î2ÚU\u0015¬\u009F\"\u008CÒ[rÎ6\u0094mÒÌõð9îo\u0097Ôæ¡ZJý/týSÐý&µñ\u007FÃÏ\u008F\u009E\n" +
                "\u0087âGÃË¸aº\u009A=×z{H\u0003FÝÁ\u0003¡÷¯0ñ\u0007\u0089ã²ß\u001AÌ\u0019\u0091°À7pkåO\u0085¿\u0012ü]ð³Z\u001AÞ\u0089\u0004²[J\u0085.\u00AD\u00180IPðy\u001D\u000F¡í]n³ñ<k\n" +
                "&§áëÉ\u0095_ækyÏÎ§¸>¸õï_çw\u008A_GlÏÃÌmLf[?o\u00977îËyR»ø*[¢Ú3ëÖÏCú\u0003\u0082¸Ã\u0007\u009EÆ8lKöuû=¥ç\u0017ú}Ç¬ê_´®\u009BáÓ\u000F\u0085|\u007F¥Å¨èwr\u0004\u0099n`\u0012\"zd\u001FÃÞ¼\u008FöØø\u0007ð§â'Ãé<Kð\u0093W\u008AÃTX\u001Aâ\u001D5§ß\u0015Ò\u007FÓ&þ\u0016ÿd×\u000Bã?\u008Dú\u0015ºÉ¦øÆ5\u0086'Oõ\u0092\u008C£~=«\u0080Õ|I¨ézsÛøWÄmu¥ÈÞr[´\u009B\u008Cg¨djüã+Èë`±\u0010ÄP\u0093§$ÿíÙ\u007FÁþ´?Z¡\u0096¾u8hüº\u009F,x¯â\u008E¥«i7ß\fü`\u00195\u000B0ÿdyF\u000B\u0015à¡÷¯0\u0093âÓé\u001E\u001Eÿ\u0084c\\¼and+o;\u001FõM\u008Fº}\u008DmþÙ\u001E%¸Ô¾'Mâë\\[¬\u0091í\u0094*\u0080ÂaÆÿÄuúW\u0082j\u0011\\köÂòóÅv\u0092¼éæId\u0001%\n" +
                "ç\u0086\u001FÞ=qï_Òù\u0006IC\u0017\u0084\u0085Y+FV\u0093]¥ä|Æw\u009Fb°8§B\u000BßZy4ûëý|\u008E\u0097ÅÞ\"ÔüEo\u001CÖ\u00171º@@ûA\u009B\u0018+\u0086À÷¦[|^\u0087M±¸\u008EîÍìî$\u00900¶\u0099[s§=û\u0006þµ\u008Bð\u0097Ãú'Äÿ\u001EØx6ãâ ðÅ¦£{om\u0015ÝÌ,ë\u001BK*£9Uì¹Ü}\u0086+ªø\u0091û+ücðgí\u0015{ð;Ç\n" +
                "\u0005Ö\u00ADa©\u0088\u007Fµ®e)au\u0013b\u009FÎ#\u000B\u001B®Ò\tÀ\u0019ö¯¯X\f\u00058û\u0019ÛÝWìÿÊÝÏ\u0094y¦>sö´Ûæ\u0096\u009D×ù¯.æ]çÇ_í8¡±K\u0016µ\u008CÌîÐB\tH·}Ð3É#¥z6\u0097ñi¾(É¥isk\u0010ÚÙi\u0091E\u0014¶òI·q\u001F}ðzà\u000FÖ¼ªçàï\u008D¼Mã\u001B¯\u0003ø\u0013Âxñ\u0006\u008B¹µ+k\u000Bµ\u009Fí\f\u0084\u008DÐ\u008Cüã\u008E6çÖ¹\u00884ÿ\u0017ØY\\Í\u001E\u0089{öX¥\tq$°\u0090cu º\u0093ü<\u0091\u009F\u00ADe\u0088Érü](Ê\u009Aå}\u001FkÿZ\u0017C\u00883\u001C-g\u001A¯\u009Dl÷[tÿ=\u000F»¼\n" +
                "ñ\u000E×Æ~:Óî£\u009Bn\u009B§F\u0089k\u0010þèà`z\u0093Í~\u0082~Ì\u007Fµ\u0097\u0083¾\u0004ü*ñ\u000E¹â{å:¬\u008D\u0018Ò¬³\u0096w*B\u008F`¤äû\u009Aü\u0082ý\u0096¾+è~\u001AÓ/u¯\u0014êvÉ\u007Fd§û+L\u0093#Í\u0091º\u001C\u009E gô¯ið\u009FÅ\u001D{Ç\u0017G[½\u009DÌ\bFÎÃ5ø\u009F\u0017ðtq\u0095\u001E\u001Eªj\u009Cm®×Öö^½Yú^\u000B\u0017\u0085Îp\u0011»Ö]:«~GÚ:\u0007ÄKÿ\u0013ê\u0097\u001E+×o\f÷Ú\u009Cí4Ê[¹9¯¨\u007Ff\u009F\u008Dº\u001F\u0084~\u0017kV\u0090j\u0018Ô®n\u0082\u0099AËlÛ\u0080©ø×ç¿Ã¯\u0013_ÞL\u008A®êHÃ\u0001Ð/½}-ðgS·Ò\u009E\tg 9æ=ã¿û#ø\u008D~[Ä\u0099=\u0017AÓ\u0092ÓM\u0017\u0097OC,v\n" +
                "5!Ê\u0096\u0088÷$Ònµ\u009B\u0099|A¯·ï\u001F.©!á\u0007÷\u0098ÿJò\u001F\u008BwÞ$ñ\u009DÁÐ¼\u0013á\u00ADB{\bß÷÷PÚ¹\u0013°ô8û¿Î¾Âø3à\u000F\n" +
                "\\x\"\u001DsÆ\u0096)<·¸\u0096+Y×, \u001C\u0002Gñg®\u000F\u0015Óê^:·Ñ!û>\u0089à¹DQ\u009C$qÛ\u0085\u001F\u0090\u0018¯;\u0081<GÀp&|³\u001Fìõ\u008B¯IþíN|´ ÿ\u009AÉ7)/³ªK}]\u00ADð|A\u0097WÍðòÂS«ìàô\u0093KV»'}\u0017~çæÜ¿\b~$%´\u0097Rø\u000FVX£ÿXâÉÈ_|â¹Ù\u00ADg·c\u0004\u00912\u0015?0eÁ\u0007ñ¯Ñ\u00ADOã\u0087Å6\u0095áÑ|.--óûÆ\u009A\u000F\u0090\u000FS\u0091Í|÷ûQÿÂ¤ñÞ\u0081uâ\u0084\u0089-¼Ed?{uad#\u0086à\u009E\u0004n8\u001B½\b\u0015ý\u0095áÏÒë\u001BÄ|AC-ÏrÅN5¥\u0018FtfçË);.h5{6Ö©ÝvgäùÏ\u0086rÁàç_\u000B[\u009B\u00916Ô¬´Z»5¦ÝÏ\u0099L~^\u001Fiç©Å\u00062\u0018²\u0010s×\u0007§Ö¦h\u008B6<²£\u001Dùæ£hr\u000EF@é\u008EõýÄ\u0092?&¾¤g#\fG\u0004gÿ\u00ADFþ\u000B2ä\u008E\u0005J©\"Ç\u0087\u001Dú\u009AWS\u001E\u0001àþUI¤&Æ)\u0003!\u00863Þ\u0083\u0019ÏÌ\u000F¶*P¹mä\u001Ct8\u0014¢=àFÇ!FE\të \u0086,\u0019cØã\u00AD\n" +
                "\tm¯\u0082Àp}*B\"LrI\u001DyéNê\u0019\u008A\u0081\u009EÙïO\u009AKV\u0004[bòÉ|\u008EyúSÔBGú²W·8¥ÚÏ÷\u0086Aè}j@Frï\u008F`2)'f\u0006KgfáÛ 4ª¨È\u0018\u009EqÞ\u009CTcïué\u0091A%\u0087Ý\u001F\u0085&ï© \u0083÷cswô©#Mè\u0019\b\u0003\u001C¯µ5Q@bN}\u0016½\u0013ödøm£|Cø\u0091\u0005¿\u0088îcK\u001B(MÌð3\n" +
                "×,§\t\u0012\u008EùcÈô\u0006¼>%Ïð\\-\u0090âsl]ý\u009D\b9´\u0095Û·Dº¶ô^ºèvà0u³\fe<5/\u008Am%}\u008FPý\u009F\u007Fcï\u0087~&ðe\u008FÄ\u008F\u008A^.¼[\u000B´Ìv¶\u0096å\b|\u0090U\u009D¿§\\×¿x'à\u000FìÏ¦:Åà¿\u000EY4Ë\u0016Ræí|ÖÏ©/ÇéX~+ñ\u0007\u00884[\u0014\u008FTð¶µ¨@Ñ,-a\u0016\u009A!¶µ·\u001FÂ\u0088?\u0088uÝ\\~±â{Í\t\u0012ûE\u0097ÏÓ¦?è÷1\f`\u007FqÇf\u001DÅ\u007F\u0090üwâ·\u0088>!æ\u0095«ÖÌ*Ó¡9>JQ\u009F,#\u001Bé\u0016¡Ê¤ÒÝÉ]ê\u007FLä\u009C)\u0093e¸xÂ\u009D8¹¥¬\u009A»o®÷jç¯ÞøcD\u00926ÒÓÃ\u009A%ß\u0096¥Q\u0005¤h\u00ADè2+Ê¾!üð.¬þ~«àY¼=>üÅyl¾d\fÃ¦@éRé?\u0018uÖÓ\u0016ÉR\tP}Ñ,c#>ýjæ\u009DñOÄÖjÖæMÑ¿Þ\u0082Q½\bú\u001Aüë\u000F[=Á9(V\u0092¾ö\u0094\u0095×\u009A½\u009F£Lú\u0088àà\u009A½5§\u0091ò\u007Fí7û(xº×B»Ô-ìWPÓ|¶v¹³ýâªú\u00909\\{×ÀÞ=ñ\u001FÄ\u007F\u0084W2Øi\u009AÄ¨\u0091\u0012mÖ_\u009A6\u0019éÍ~ÁëðèúÌ·\u0012Ýë\u0017\u001Ab¼l[ìÑï\u0004\u0091÷Jú\u001Aü×ý¸~\u0011ê\u0011ß_ê>\u001Eµ\n" +
                "\u0011w3XL0ÍþÜ\u007Fá_®ð\u0016w,EoªâìÖ\u009B\u00AD?Êþ\u0087ÕP©V¦\u000E\\\u008FÞ\u008AÒÇÇ¾&ø¯¥üPñÞ\u009B\u000F\u008Ct¶_.\u007F;QKT,³Ä\u009C²\u0001Ø\u0092ük\u008Dñ\u0017\u0088<!\u0016\u009D\f:n\u0081qg\u0013jSÝyöê Ã#6\u0002g®\u00146ô¬»«ÍK@ñêIoyyn¯t!¹[l,ÅKd¨'\u0080r\u0001ÏµKâ8.üG©ÙxV/\u0014YÉ\u0014÷Ì\u0090Á\u0017.Yß\u001C\u009E\u0084ç¿NõýK\u0082Ááð´a\u001AzF×µÞ\u0087äø\u009CÇ\u0015\u0089«VuRs½µKËÍ?¸Éð\u001F\u0082µ\u009F\u001FxÅ4\u009F\u000BÞÉý¥sp\u007F³míÓ2ÜK\u009F\u0095\u0017\u001D\u000EyÏjö?\u001D|pý©>\u001C¤^\fø¡áëÍ;P¸ÑÆ\u0096&ÕâÝ<ö£\u0082\u0092«çrò0z\u008Cpj\u0097\u0087¿g\u001F\u000B\\øÇ\\ð·\u0085üao\u001Eµ£2Ê\u009A\u0084úÔV¶Ö°©\n" +
                "Ò\u0017'æ}Ç\u0085SÒ³þ/i_\u0010ü!â½\u001FÆ~1ø¬¾!¹\u0087\u0007AÕc¸\u0013\u0099¡\u0084\u008D¬Cg\u008Cñ\u0086\u001C\u0081ZÔ«\u0086ÆÔ\u008F2M5¥Ó¾Ënë½ÿ\u0013\u009A418:siÙõåi\u00AD_Ú¾ÞVûÌ_\u0084ð½§\u0088§ñu\u0084vëw§\u0089\u0017í-vU\u0011\u008A9ó\u0010\u0082\tn>EÎ7\u0001\u009EµÌxÅ¾ BÒk\u001A\u0097\u0088/\u001E\u001D[t\u0080ÈÛ|ä\u0012\u0014-\"öbPdz\u008Aîm5Câ\u009BM[P¹Ñm\u00AD/üá~<\u0088\u0002[Ü.2cÀâ>A`G\\\u0095\u0015Êëâ÷ÆR5ýÂD·2\\\u0098\\\u0087Ú¡ÈáB\u0093\u0091\u009EäñÎkzsj\u00ADä\u0095´ìíý\u007FHæ«F1£î7}vÐÂðþ±\f³\\\u007FjjÒG<0\u0095²\u0091\u0097#~1\u008C\u008EFp\u0005}9û8üXÓ|Gci¡k7°ÛÝÚÛ/ÚZTÚ t/\u008Eç\u0018Å|»®xgYð6¼Ún£\f\u007Fi\u008E@c6ó\t\u0015\u001F\u0083\u0090FCbºß\u0087¾/\u0093D\u0096\u001B{qo<M¬Æú\u008C\u009D.®\u0081\u0018\u0011¨=\u0010sø\u009Aó³Üº\u0086g\u0081|¾©¯Ðôø\u007F2«\u0096ãmSE³Zßþ\u001Fô?E~\u0010x¢ãÄz¤^\u001EøWáIõ)¤p¦þxÉ\fzd(¯¨tÝ7þ\u0014=ö\u009F\u000F\u008B§\u001A\u0087\u008Bo!YÞÅ\u0086F\u009E§ eþ\u0016#·¡¯-ý\u008Cÿl¯\u0086\u009F\u0006~\u001FÁ£ü)ø7\u0014^,\u009Eßdú¥üË7Ù½ãmSîy¯Aø[\u0016«ã¿\u001EÏâ_\u0012YOwy\u007F?\u0098Ó0,òHOaÔ\u009AþSÏ¥\u0088xª±«IÂ\u0094o»NSû¶_\u009FcöIÓ«:N¤ãË\u0004´×WþHúOáçÅ\u007F\u001C´cY\u0092ýÒw\u008C.\u0015FÔ_î\u0081Ð\n" +
                "õ\u007F\n" +
                "ülð\u009CZgüM4©çÕù\u0001máÛý\u0095õ®\u000FCý\u009F¾.O¦¥Ä\u009E\u0013¸°·Ú\bB¹\u0091\u0087ÓµmÅð³Ç^\u0002Ùâ4ðåÔ/\u000FÌ·RE¸©õÅ~?\u008DYf&m&¯Ù;|´>\u001F\u0014¨JmÝz\"¯\u008E<eñ\u0013Æ²\u009B)í§ÑtÇ'Î¼\u0096\"\u0004q\u0081ÉÎ8À¯\u0098\u007Fh¯\u008A^\u001Eñv£\u001F\u0084ü\u0003¦Ç\u000E\u0089¥ü±\\\u0015ýíü£\u0086\u0099ÛÜç\u0002¾\u0095ñßíA©ë^\u001E\u009FÃW\u009AÓ¥ÖÃ\u0013$zr°\u0098\u0011\u0083»=+æ?\u008B?\tµ?\u0006è¶þ-ÖìeÓÛUº?Ùð´{Vd\u0003.ûz\u00808Çc\u009Aþ\u0093ú.×á\u009C\u0007\u0088T\u0016o\u0086n¼ýÜ7/½\u0018ÍÞó\u0094l\u009B|¿\f\u0095Ô\u0015Û]Wç\u009E PÍ*püÞ\u001EJ\u0010\u008E³[9.Éþk©æ¥6á\u0089 uü})\b ì-ÇlÔì¡\u0082\u0086bGð\u0080:ýi\fJò\u0016vä\u008AÿT\u0093gó\u0089\u0010,\u0001,A\u0007°¡6\u0092ARséÛ\u0015!] &xÏ\u0018\u0014á\u001Aó#\u009F\u0095º\u0001ØûÓÑ #hä$ \\ú\u009FJU\u008C\u0091¶AÈ<\u0011Sg`d\\\u0091êE0\u0007iHÜ@Ç§\u0014G¨\n" +
                "\u0001\u0015\u008A:\f\u0093Á\u0014åD,ËÀÉïO@¾g \u000FLö§\u0094Wo\u0098ñ\u008C\u009Fz/m\u0085ªDQÆpC\u009E?\u0087\u00034¢6-Ï\u0007\u001C\u0081S,m¿\u000E:\u009E\bíJU÷\u0015. g#4âõ'S\f©É\u0019#¾(A¼oTüI§\u0088N0òã\u008E\u0094,~\u009D=qOFl¤\"¢\u0099\u0001<c½Kkw5\u008Cëui+Å,x)\"1\u0005O¨#¥4\u0016+¶.\u0098äÓ¾ïÍ(\u0019Ç\u0019¬¤£88É]1¦Óº=KÃ_µ§Å\u009D+LM+Y×fÔíã\u008C\"5ÌÌ%UôÜ\u000F?\u008DBß\u001D\u001Bí\u007FhÒ´émã\u009ELÝ@ónV\u001EÀ÷÷¯5C\u0096Ë #ûÀW}ð'ào\u008B~5ø¦-\u001BÃú`\u0096\b]Zúi\t\t\u001A\u0093Ó#¹\u0019À\u0015øo\u0018øKà\u009E\u000E\u0085|ó9ÁR¡\b'*\u0093MÓ\u0086½\\c(Å»½,¯{[Sìò~+âåR8L\u001DYI½\u0012iIþ)»~\u0087¢x?ãGÂÝ`Eiª\u0099lnJüÍ\"~ï=:\u008E\u0095î\u007F\n" +
                "<\u0005á]wG:õíËÏm)ýÔ¶Ì\u001DBûã¥\\ð×ì]û1|7Xµo\u0089\u000F\u0004·!\u0001{Yn\u0089L\u008FD\u001Cãë[×_´\u0017ÃO\u0003Z¿\u0087~\u0015ø\u0002(àT*\u0092yAS>»{þ5þiø\u0095\u0099øc\u008FÅòp-\u001CRW÷\u009DW\u001Fgoî^óûÞÝ\u000FÞr*ÜM*n9\u008B\u0083}9n\u009FÎú}Èæüað/Ã\u001A½³Ëáo\u0011|Øâ7<\u009Aø\u009Böàø/©ZÚ\\i\u009A\u0084\n" +
                "æº3Û:°ÝÇq_rÇ¡|mø\u0087¥M«éZ\u0011\u008A'ËÇ9·\t×û½Í|¡ûbü\u0011ý¡õV\u0090Ç®XC2BWý6Ý\u0099\u0094z\u008Aù®\u0012ÆTÃæq\u008DJñÓ»Õ}ÇÞåUZ\u00ADË*\u008Bõ?\u0012\u007Fh¿\bx\u0093Ã>2½}vÆkwçì÷Evï;\u0080ÉÇ|f³üqð\u0087Âþ\u0003ÕlßHø\u009B§ø¢\u001B\u008D2\u001B³q¥\u0096B\u0085Ð4\u0088Á\u0086T¡Êû\u009C×ª~Ú?¾3i^)\u0093Ä>1ñ\fz\u008A.T$2\u0014\u0011\u0081è\u0086¼7D:õÝß\u0093cc /\u0012GpPîi6«rG§¨ö¯î\u009C\u0087\u0017\u001CfSJtê'e\u00AD¶ÛÏSó\u008Cë\u000Bõ\\âª«IÚNé¿]ôÑ\u0090\u009B\u009D\fkÎ\u001EÏÈ\u00022\u0096£\"O/ Ü\u007F¼q\u009Aí4O\u0087\u009E5:$×:Å¼°é\u001B\"\u009Aîy\"Vf\u0084?\u001B\tç!sÂñë\\\u0016©oc5êý\u008FN\u00104q(Ü¬q'»g¡Ïjô?xêÉü\u001F-\u008F\u0088|u%½Å¾ÛKIfrâ+vÜJF\u009Dvç\u0092@ï^®!Î4T©ùo©ça%JX§\n" +
                "\u00AD[_%èöý\t|Uâ\u001F\béÿ\u000FSÃ\u009E\n" +
                "ÔæºûuÀY#\u0091\u000F\u009Bä)?3\u001E@ù\u0087\u0003Ò°ôûf¸Ñ®4\u008DLÁ7\u0089\u001DÅá\u0087zÆ:\u0082ÅyÎr=úRÍá\u009B\n" +
                "6;{}\u000Eéî¯'U776òü§Íû¨\u0017Ø\u001F\u0098v\u00AD\u008B=GÄ2]ßË¤éë²A\u001C\u0097V\u0088Ê©$Q\u0015T;qÉÈÏ×\u009Aæ¼)ÇÝÖúÝÙk¥\u008EÚ\u008AµY¯h\u00ADd\u0095\u0096ªÖ\u007F\u008F}NoY{_\u0002ë\u009A\u007F\u0089<\u001CèÈì²ÀÒ¯\u0098m¦\u008EA»\u0001\u0086\bÈ\u0004dt8¨u;ÿ\n" +
                "kW0júKÉ\u0005üs\u0099e\u0095£ËÜHç,N8P\u001B8ÅuPi\u008DªxvÞ]kMµ\u008Bìs\u00873Û±\u0013>á\u0090\u0084}Ðr0k\u0097×¼?\u0014¾ \u008Em\t\u0084o#b;\u0010á¤\u00893Æâ8ÝN\u0095Zu\u001D¥º¾¿çò&x\\E(sÅ.Wm:¯5ÙÜýIÿ\u0082;þÏ\u001F\u0006>;iÖö><ñN½}«n]ÚN\u0097E\u008F¾é$ô<\u008FÂ¿a¾\n" +
                "þÊ?\u0005~\u000Bªjþ\u0018ð±7H¿º\u0096òo5£>Ùïï_\u0099ÿð@OI\u001A·\u00895qxâÖ$KtÒ\u00ADI\u0012ÈG\"gè8¯×+o·\u001Bu\u0081\u00AD\u0092Ùùb\n" +
                "¹ëø\u0013ÅÜ×\u0019.)¯\u0086§Z^Îú«ÙzY[Cï3\u009CF\"*\u009D\u00057nXÝz¯êæ\u008D´ÞafQ\u0096#\u0093\u008E\u0007µU×\u0012\u001B\u0088ZÖ[é\u0014°ÆØ\u0086I¬ë\u009DzåY¬¬\u007Fv±\u009CI'\\\u0013Øzµ`|@ñßü!\u001E\u001B¸m2æ3«M\u0003}\u008A\t\u009C\u0017$\u008F¾G ¯ÉèaªT¨\u0094w{\u001F;ìÛ\u009D\u0096çÍÿµ¯\u0089¼\u0015ð\u001BÅÖ×Zm²j\u001E ¹\u0006\u007F²Ï\u0012m\u0085AùL\u0098ã'°ï_-üXø\u00ADã_\u008C>&\u007F\u0014øÓQ3ÎT,Q Û\u001C\b:\"(á@ö\u00AD\u001F\u008C#Æ7¾2½Ö¼Y©MyqupÌ×s\u001C\u0097>\u0087Ó\u001E\u0095Ç´l7>Ò?Ù\"¿Ø\u009F£¯\u0086\\\u000FÃ|#\u0084Îp3\u008E+\u0017R\u001Eö#w\u0016þ*pOøj?\u000BVRv»?\u0002ã\u008Cï:Åfu0X\u0094á\b=#Ýtmõ¾ë¡T\u0085\u008EM¥÷\u000E¼Pa\u0090.Üdw\u0004T\u008E¨\u0002°Lduô§\u00072\u0083µ\u0088\u001E\u0084Wôºmj|\u0010Ä$aKà/\\\u007F*nÜ\u0082û\u008Fbx§´Mæ6Wåe¢\b\u009DPùO¸\u0003\u0082\b§dÐ\u0086®ó\u0081#\u0080qù\n" +
                "]¤\u0080c\u0004)ýiÏ\fª0\u0084\u0002Gz\u0091\u0011\u00802F \u0091Ø\u009E\u0094ß/-Âú\n" +
                "*\u0082#\u0019\u0004\u009C\u000EqMH°Û\u0018õ©B>|¶\u001C¯<÷¥âQ\u0091\u008C\u000E6\u0091ïBºD§Ü\u00100Â#\u0013ÆO\u0014ÇUSÇ#\u001CqSÁ\u001EÕ*Ç vì)Ë\u0010$(lqÕFsJ.Ì/m\u008EpôÜ\u0007×\u0014ò\u00190À\u0090:\u0001H¨\fe\u001Dð½Ï\u00AD/Ê§k\u000FÃ=«g{hl\u00821\u001Ey\u0018õÇ¥?jn\u0003 \u008Cð@äÒm`1ã'®iñ¤Dî\fAÇAëY4\u0083Ìì>\n" +
                "ü,Ö~.xîÇÂ\u001A4NRY\u0097í\u0093(Ï\u0093\u000E@g?\u009F\u0003¹¯¿<\u0019ð\u0007Á\u007F\t-ÛÃß\fo/£¼\u009EÝMÄK.Ô\u008Cã\u001El¯Û×\u0015ó·ì!ák\u001F\t\u0099>%ë¾>Ò´\u0098¯Ñ ¶²¸\u0099D²l?\u007F$ü¼ç\u0019ú×ÖZ>\u00ADðÃ^O°Þ|GÓ¦\u0089F÷±Óîó¿ý©\u0018rÇô¯òóéuÇ|E\u009Dñ|²\\,§ý\u009F\u0085K\u009B\u00963ä\u0095Wñ9»rÉÁûªÎÉß[¶~ëáþS\u0085Áe±ÄÍ/mRý®£ÒÝlÖ¬â5/\u0083¾\u0015º¾6¶~&¼Ö5f?¿h¢ß\u0012·¦k¼øiðbÊÊÝ®¼qá\u009B\fÀA\u0085\u009A!¸û\u009AØÓ<sðÏÃ¨l´6HâE,æ\bv\u0080\u0007©êj¦¹ñ7N\u0097C\u0097ÄúÕÀ·Òa8HÁù®\\tAíÓ5ümW\u0015\u0098b#ìÕÒ}^þ\u008AÖ?Gý÷-¢\u008E\u009E÷PI\"\t\u0004ÂÞÑF\u0006Î\u000B\u000FoA^uñWÅ_\u000EtË\u0019\u0017\\Ñm¯<Å(\u0011¡\u0012;\u009FOZæï>!ø·âLÌ4(\n" +
                "¥\u0090æKÉ²\u0015\u0007·©ö\u0015æÞ8ø\u0081e ßM¦xd´÷\u0091äO«Ý\u0090Ï\u009Eá\u0007D\u001F\u00ADuåù=Ol\u0094·[¥ÓÕ\u009DXl<½¢]|¿Ìñ_Ú\u0007þ\tÙðËö°¿\u0099u\u008D6\u000F\biÓä\u008B\u008B\u0087ÌÌ\u000FxãëÏ½~/þÝ\u007F\u000F|\u001Dû.~Õ\u009E3ø\u0019ð\u008FÄ/ªx\u007FG¼\u008EÑuEp²ÈV %@G\u0003,X\u001F¥~À|bøß®hÑÞÇáÿ\u0010K&£u\u0019\u008D¯\tÞbú\u0013Þ¿%¿n/\u0085\u009E\u0015ð¾»sâ\u009B{ûë½VþèÍw$¨\bfc\u0092Iõ$\u0093\u009Aþ«ð{\u0017\u0099a³\u0007G\u0017YºR\u008F,!k«Ý>fÞºZÛõØõ8\u008F(ÇUÉþµux|\u009Dºë¿]®|ÿ}\u00AD&¸\u001FWÔ®\b\u0016°\"\u0098ÖÜ(\u0090¨\b\u0089îv¯'¿5gU:\u0016¯¥éz7\u0086¼*loc\u0088¾¯y4¹3>I\f£ø\u0010.8¬ýRÒîÎâ-&àÇ6é#\u0095\u009C1\u001B\u0081\u0019Áú\u0003SKâ\u0096²Ôæk{TÄ¬UÁ\u0019\f `)öÀ¯éy-\u009C=V¿/\u0099ùw2WU\u001Dº=.þ]º#v8<\u008DB-\"ÃU\u008Aâ-*Ù¤\u009EâÇ;ZF<\u0090O$\u009E\u0006i5X5_ì\u000B{û(¥\n" +
                "¨ÌÃqãÊ1\u001F\u0097kg\u009FzÅÐítS,Ï\u0006¨c\u0083Ë\u008Bí1;aÛç\u0001¶\u001EøäóZÞ$ø\u0081q\u007F;i6¢?ì¸¤\u0090i¶ìª^4 /\u008E¬G9®iB~Ñr«÷¿õÜë§:N\u008Bsvº²¶½\u007F\u0005m;êh\u000F\u001Bëz>\u0081\u007Fee\b\u009EÞþ\u0018\u0004²\u009C3Ã)Ç*\u0007\\\u0090\u007F\u0006>µ\u0087au`u(.l\u0016æ+±*\u008B\u0081ü8Ï8\u001DEY\u0096ítÛ04ÕÙ:Ú¢\\@änb\b8\u0007¨ë]ïÀ/\u0087ñ|Bñ*ÌÚ\n" +
                "JÑÊ<äi\u000Eá\u0093\u009C·¯Ö¸±x\u008A\u0018\f4ëÍi×ò=<.\u001B\u0011\u0099béáã-z^îÚßGÜý ÿ\u0082\u000F~Òþ\u0010¿øc\u0017Â5ø\u00AD§i\u001FÙÿ3i7ÖI\f×\u0005¿»/\u001EgãÈ¯Ó{\u0001ct¢{mHÎ\u000E0ÈÃ\u001F\u0098¯Í\u001Fø&\u0097ì\u0013ð¿ÅZTsxÇáÕ¬\u0096ße3[ëº>¤À\u0087ÀýÛ\u000EÄué_nxwö2ð¿\u0085ÓÈðÿÄÏ\u0013ÛÃü\u0011\n" +
                "@\u0090£ñ¯ó×Ä'\u0090âx\u0096½Z5e\u0017'v\u009CSW{ê\u009Dþõsìs\u009A8\bâ-í\u001A\u0092Jþî\u0097ù;ýçcñ\u0003Ç>\u0018ð\u0015¡\u0096i£\u0096é\u0089òmc ±oS_4x¯ÂÐx«Æ\u0013|Eñ/\u0089µcrò\u0006\"+\u0090«\u001A\u000FàQÐ+ÕüGû!x\u0082B÷:\u000FÄ\u0019%\u00979\u001FnRY½·W\u009BøËÁ~:ð\u0004¯kâÛ2±\u0093\u0088æ\u0003(ÿC\\Ü/\u0099Ï%«)å¸\u009EZ\u0093\u008B\u008C\u009AVn/xÙ§£ëÜàXL·\u0013K\u0096MM\u00ADuºÛ±Òiß³\u0017À/\u008EÚd\u0089iâ\u008Bøo\u001D\u0001\u0092\u0017uÞ\u008FÓp\u001Dó^mñsþ\t\u009Fâ¯\u000Eéòë\u001F\n" +
                "|G\u001E³årlgO.b?Ù=\t«v\u009A\u009DÞ\u0089t\u009AÎ\u0095ª\u0098$C\u0094x\u0098\u0083úW±|.ý¢5¯\u0018Úÿcj\u001A½\u008CWÑ\u000E\u001EâÝ\u0082Ê\u0007|ô\u0006¾û\u0084|Rñ?ÃZ®y\u000E5º\u000E\\Ò¥8©A½µVººÒñqgÌç¼5\u0095g2¾.\u001CÍ+)j¤\u0097¯ù\u009F\u009E\u009E%ð®»á\u008Db]\u001BÄ\u009A\\öwp>Ùmç\u008Bk)üzÖvÐcÈÎHÁÍ}?û}x\u009FÂ>,\u0093OkmWD¸Ö¬æeº:d¥\u009FË#\u0018c\u008EÆ¾edp¤F¹ç¥\u007F«^\u0011ñÖgâ'\u0004PÎ1ø9ajÉµ(;¤ÚûPæI¸Ij¯æ®ísùÛ\u0088ò\u008A9.i,5*\u008AqÑ¦\u00AD÷;uD1\u0094bB\u009Cf\u009Däº\u0012Uð?Ù\u0015 \u0089|Õ\u001Bq\u009EA\u0014¾Vù\u000E~Q\u0083\u0080\u0007jý:çÏ·Ð\u008C\u0002áI\u001Cvã\u00AD<+\u0014\u0005\u0018\u000F \u000E´õ\u00822£iåz\u000FJtQI+\u009FNzÒ\u0010Å\u000B\u0014\u0084\u0015\fÞ´¢.r~ïu\u001Diá\u0014·\u009AW\u00901¶\u009E±Hï\u0098ÀÎz\u0011Á§¢\u0001\u008Bn¯òïÉ\u0003%GJU\u0003nÕÉ9íØT\u008B\u001EöÎ0OaM<\u001F\u0095vö<Qe-ÀæØ\u0002½\u0007Ð\u008A\u0019B\u0080§\u001Cõ#½<\u0006\u008Cç%\u0085(\b\u0013\u0095\u0003×\u0015®¦©è!\n" +
                "\u0007Í·=\u0001\u0007\u009Ar\f\u001Dª \u0093ÔÓ°\bÀ@9ëí@I\u0011Á\u0004\u0091èµ-h\u0017ÒÅ\u0098%um¬ùÅ}\u008BðKà]ïÂï\u0001C¯xÇÏ¶}B$¸û>\u009Dnf¹\u009DJ\u0082\u0006: Ç\u001C×ÆÑ8\u0082U\u0090£eH`G\u00ADz×\u0085¿lßÚ\u000FÂD\u009BO\u001DÏq\u001B¯\u0005Ú,\u0088Täp0+ùïé\u0007áÏ\u001Aø\u0095ÃT2¼\u0082µ:q\u008CÜêFnQö\u0096·,SQ\u0092I;·uºGÚp^\u007F\u0097pþ:uñQ\u0093mY5gnûµ¾\u008Bï>\u0091ð\u008F\u008F<7ã\u001D^çÂ\u008Fá{\u009D&Ê=¯5Ö¡r<é°ßw®\u0014w®óTðw\u0086<I$Z§\u008B<u¦\u000B+$\u000Bc¢ÛÞ\u000F.%\u001Fß#ï\u001F\\WÉ\u0017\u007F¶\u0004\u009AÍ\u008C\u009FÛ¿\u000Bô\u0099oØ|·öìÑ\u009DÞ¥G\u0006º\u007F\u0080Þ#×¾8ø½4\u000B\u001D\u001Dm4ûxÄº\u0095Üdb5ì\u0007ûLzWð\u0007\u0015}\u001E<Ká\\¶¶i\u008FÃÆ\u008D\n" +
                ")¹MU§$\u0092Ý«Ë\u009Aïd\u00ADÌÛ²W?jËøÓ\u0087³Jð£F³ç\u0096\u0089rÊÿ\u0091ê_\u001B~-Yè>\u001DþÁð|¤Ã#\u0018\u008DÝºm@\u0007ðGýM|Õâ¯\u0017êÚ }2ÂFRää¯_Ä×§~Õ\u009E\"K?\u001AGðûÃ¶\u0091[Øé6èªøå\u008B\f³\u0013þs^E,\u008D\u0016Õ\u0088\u0082\u001CüÄ\u000F\u0099Ïô¯\u0081Èðtéa#S\u0097Yk®ÿ3ô\u001C\n" +
                "zXzjÊí\u009E}ã-\u000EkU2L\u0004\u008Cs\u009C\u001F¼}+É<[û3Ãâ)\u008F\u008C<W¢¥õË\u001Fô\n" +
                "=×ä\u008Cö$w5õ\u0096\u0097ðÒâpºÞ\u00AD\n" +
                "\u0088Âî\u0011°û¾\u009F\u008DzwÀÿ\u0084z\u0016\u0096×\u001F\u0017<\u007F¥FöVHWL´\u0099~Yeìpz\u0081Ô×Ò.&yU':o]´Ý¾Èö%\u009CB0³\\ÌüÕñ÷ü\u0010ëâW\u008D?gÿ\u001F~×7\u001E,µ²\u009BEÒ\u001Aú×C\u0092Ô\u00838C\u0097EÀù@@p}«àÍwödø¿¡ü \u0093ãV\u00ADà[Ø<.\u009A×öbj¯\t\u0011\u009B\u0083ÎÁï\u008E}9¯êGNÒ5\u000F\u0088\u009F³î\u00AD¡^éH£Ä¶³G\u001D¦Ð\u0014@ÿ»\\\u008Fq\u0093ø×Cã?Ù\u001Bàg\u008Dþ\n" +
                "¯Á\u000F\u0015|9Ò®´\u0001jÞn\u009EmTFÓyELØ\u0003ïòNïZô²O¤\u0006m\u0092ÉÒÌ)ª«Ú$\u0092ÑÂ\u009AJémwÙ³â³<\u0016[\u008C«:\u0093\\²mü;zµ×Sù\n" +
                "\u009AÁ\u0099ü\u0091fÑ²¶\u0013h?\u008Eju³\u0090\\ÂÈ\u008B¾6\u0004\u0092¼\u001Fjý\u0094ý®ÿà\u0086ºwü,\u008F\bÜü#Óm\u0092ÃP\u0086[[UF\u000B,òÆw\u0001 \u001C\u0016ÆG¸\u0002»\u009F\u0013ÿÁ½¾\u0001ñ7Âï\n" +
                "üJøwáË\u008D/Åva¢ñw\u0087nXì»\u0018#|@ý×\u0019Ï¡¯Úß\u008E<\u001B\f=\u001A³\u009BJ¥ÿí×ª÷»'k_£Üó?Õ'\u0006¤ñ\u0011³i/NítÕXüRð\u007F\u0087õ\u000Bß\u0010\u0099µk\u001B\u0089!l4\u008F\u0018ÉôãÔWß_°\u000FìGâo\u0089\u0097\u0016\u007F\u0010¾\u0018NÍ5\u008DÂ\u009B¸LeÍ³gîÈ\u0007;\u0018g¯\u0018¯«>\u001C\u007FÁ\u0016\u007Fá\u0018³º¹¿Ñ$1Z\u001F+VÓÞ0ÒB§\u0094\u009A1Ô\u008Cr@ô¯ ¿eÿØwÇ_³\u009FÄt\u008FÀ\u009E#\u0093K\u0096òÛ6ú\u00AD¤GÉº\u008F\u0019T\u0095O\u001F\u009FJø>3ñs,ÌpS§\u0080ª\u0094\u0092ë¬Zê\u009FË·\u0093>Ã'Ëð\u00195\u0019ÉT\u008Cên\u009B]|\u009Eç¶|\n" +
                "ø\u0083ðãöWøbÑ|jÐ\u0097ÁÒ$b[Û×¶cnÀú:\u0083\u0091ß\u009EEnéÿðQoÙ«[°þØðO\u0089¿¶´õ+¾òÀeB\u0093\u008C\u0080y8=©/¾!êºþ\u009Dsðßã\u009F\u0081íuHÈ1]ÆÐà²\u009E7\u0001ÐñÎkãÿ\u008Cÿ°v\u009Bð7[¹ø\u009Fð§X¾>\u0015½¸ó\u001FNHöý\u0088·;Xw\\ôn\u0095ù\u001F\u0086ü-Â¼kÅ±Àç\u008A~Ò³÷\u0014*Â\u0011\u009BþU)§i5¤bä®ôNú?\u0099â:Õð\u0098)âÒ´\u0096\u00ADë(Ûºå×O\u009E\u0087è_\u0082ÿh_\u0083\u009E=T\u001E\u001Bñå\u0084²0\u0004E,Á\u001F\u009EØ5Òk\u001A6\u008Bâ\u009D)¬5Khn\u00AD¥^\u008C\u0003\u0002=A¯Ê·¶\u0082\n" +
                "5u\u00AD\u001FSvòHYÐ\u009D¯\u0011ì~\u009EõÙ|+ý«>-|'Ô¢\u0097KñD÷vHÃÍÓï$/\u001B\u008Eã\u009E\u009FQ_ÐYÿÐª¶'\u0007<g\u000Bf2ç\u008Dÿs\u0088\u0087,Ô\u0097Ùö\u0090ÒûYòò»ÝJÚ\u009F\u0097á¼D§B¼a\u008D¥dö\u0094\u001Dâ×{oëÕv>\u008Fý¦>\n" +
                "xãáö\u0081?\u008C~\u0014Coycn¥ï,æ\u008BtÐ¯]Ë\u008F¼\u0007ç_\u001Cëÿ\u0010|]â;¦\u009FSÖ¦Ïhâm\u008A?\u0001_~ü\u0015ý«¾\u001F|mÓÅ´l¶Z\u0088\u0088\u008B½2äç\u00820J\u001Fâ\u0015ñ'í3àÍ+À?\u001BüEá\u009D\u0017jÚÁ|^\u0005\u001D\u00148\u000F³ðÎ?\n" +
                "úï¢\u00AD\u0019ä¼U\u008Dá\u000E(Ê!O1¡\u000Fk\n" +
                "³¦½£\u00872R\\Îé«É8N:5uwdsñýzøÌ¶\u0096a\u0084Å9Q\u0093åqRÒöÑÙzj\u0099çÏ$®Ä\u009EK\u001F\u009B\u009AcÇ*ã#hÏLõ©Bn?0PM*Û\u008C\u001C0f\u001Còký\u0001V\u008D¬\u008FÆäõ\"%UT0Ã\u0003Ïz6\u0099$\u0005G }ãRá×©RGÞÇ¥\n" +
                "\u0098lì?ð\u0013Ö¯DCb\"\u0080ÛC\u0082}\u000F\u00AD\u0001\u0015\u0098©aÀ\u0019#±§ùeH\n" +
                " \u008E»ºæ\u009E A\u009E\u0006sÐqH\u001B±\u0010\u0081\\pF~µ.F\u0002\u0093Èôý)L%°Ò®sÓ\u001E´õ\u0085C\u0007$\u0083Ð\fÑqsv\"Ù¸\u0016rO4íª2Ê¤\u009E\u0087\u0007¥HÁ\u00867`°`sN\u008C\u009C\u0090 g¾i-\u0089¾§(\u0001XðÌ\bè\u007FÆ\u0080ªÍóc éR\u0088Ê\u008Dì;ð1H\u00123»÷c\u009E>¾õÐ\u0099Ð\u0005\u0017!\bã4Ð\u008CX¨m¤q\u008Fj\u0093c\u0011åyX\u001E¤Ó¥\u0084\u0098Õ¹È8È¨Wî\u0003e\fªÛ\u008099©P±\u0019Ç\u0004à®(UG`Xñ\u008EW\u0014\"aÎ\u000B`\u008E3ü©?y\\\u0007A½®\u0004K\u0011l\u008C`WÒ\u001E\u0019¼ñOìùà\u001B+M6ÞÚÏíVëq©ÝÎ~y.\u001C\f\"\u008E¬\u0011p>¤×Î\u0096òËgy\u0015ÒH\u0003FÁ\u0095¶çi\u0007#\u008EõìVÿ\u0017ü9¬y^1ñW\u0088æ¿Ö \u0084,vZ\u009D\u00881FÃø\u0090/\u0007ñ¯å\u009F¥\u000ESÆüAÃøL¿&ÂN¾\u0019ÍÊ²\u0082æ\u0093jÞÎ.+^[ÞMÙ¤Ò?Iðç\u0011\u0092àñµ+ã*¨M+C\u009BEg»»Òû/C\u001BÆ~1¾ñÆ±ç%£\u0096#2ÜO\u0090Ò\u009Fï6\u007FAé^\u0093û5~ÎÚ§ÅMVçQ·\u009A\u0011\u0016\u009EªÒÏp0¥Ø\u009C(ü\u008DyTÿ\u0011´ÍwÄ\u0012jÞ\"i\u0098\u001Eq\n" +
                "\f±ì t\u0002¾Æý\u008E5\u001B_øT1O£ÙÃ\u0015Æ¯xï\u0014i&æT_\u0090\u0019\u000F¨Á?\u008D\u007F\u000Bq¯\u0004qÇ\u0007ð¯ö\u009E7\u0003,='(Á9Ù{Ò»²\u008B÷\u009D\u0092zÚÈý\u0097\tÄ9V2»Ãák)Î×ÑÞËÍì_³ý\u009Aü\u000F¤¡Ô|S«½Í¦\u009E\u007F~\u008A6$Ò\u008Ep=\u0087O\u00ADrWÏÿ\u000B\u009BÇÑø_M·û/\u0086ôeÝ|ð\fG\u001CKÿ,ÁîÍ\u008CWeã»í{âÆ²ß\n" +
                "<\u0002\u001A->ÄùwÚ\u0093ýÑÏÎÙîIÎ\u0005ljS|&ý\u009D¼\u001Di\u0006¿p\u0096ö¢U\u0010Ûõ\u0096þã#\u0004\u008E§\u009FÂ¿\u001BÁÿhâ«B\u00958Ê®\"zB\u0011Mµ~¶]_EÓ®\u0088ôþ±\u001C<]J\u0092·èv^\u001DÒàµ¶¶Soå\u0099B¼p(â(\u0010eWù~u§¬êéam=ìÄ\u0004\u008EÕÝ¹ú\n" +
                "Íðö§«>\u0091'\u0088õËO\"[ç&\u0018[þYÇ\u0081´W!ñ;ÆÑG¡ßÁ\u0014êwK\u001D·^¸\u001Bßú\n" +
                "ù¸aêb1<¯¸£\u0019U\u0099\u0095ðËX:æ¯m\n" +
                "ÌBY4ï\u0014³[³(%\u0012HÏON¦½¹?u(Wçv~oq^\tû;§\u0099â»{\u0099872¼¸õ8$~\u0095ïZ\u00822ÀdNJ6áU\u009CÅC\u0014 ¶·õø\u008F\u001Dük!\u0092ZÚÛÌò\u0088\u0014\u0019¾û\u0005\u0019aîk\u0007Ä\u0010Ehèî¸\u0016î\n" +
                "\u0091Øv?\u00ADlk·2[èó_¤{\u008C\u000Bæí\u001DÔrGåX~,ÔìçÑF§\u0013\u0087\u0089Ðr;«\u000E\u000Fùô®\u001C:\u0093\u0092\u007F#*\u000ENF¦\u009B¢éz\u0080\u008BQ½°\u0086i\u0091NÉ\u0099\u000185Sâ7\u00854]wÂ×vº\u0086\u0099\u001C°µ«E<%8x\u0088ä\u007FQèEIðÒigð¤\u001EnKE#ÆÄ\u009E 1þX\u00ADÛ\u0098ÖX\u009A)@*Ã\u0005qÖ\u009CkVÂb\u0094á&\u009C\u001DÓNÍYôìgQ·6\u009E¨üªø¹àé¾\u001DxÿUð¢\u0096xà\u009F\u00101é$DåOäk\u0098\fûJ\u0090\t\u001DÍ{×ííàçð×Å1 @VHð¤\u000F¼½TþG\u001F\u0085x,J\u0014ï~N:\u0001_î/\u0082¼Y>8ðË-Íë>j³¦£7ÕÎ\u001EãoÕ«üÏæ®-Ë¡\u0095gÕ°ôÕ¡{ÇÉI'o\u0095ì^ðß\u0088u_\tëVÚî\u0091pð\\ÛH\u001A6Ïæ>\u0087¥\u001E/ñ>\u00ADãO\u0013ßx«Z\u009CÉu\u007F;I1=7\u001E1ôâ©\u009DîØ\u000B\u008C\u008C\u0011\u008A\u0019q\u008E§\u001Cæ¿Iyf\\ó\u0015\u008Ft£í\u0094\\\u0014ì¹¹\u001BRq¾öºNÛ\\ð\u001E'\u0010°þÃ\u0099ò^öé}¯nö#@À\u0018÷\n" +
                "ÊOjV\u0089Ë\u001E8ÀäÓü²X¢GÇSJ±¨\u0007h9î3ÐWyÎB±\u0095È\u008F 1ä\u008A]\u0084±Û\u001E=É©\u00184\u0083j¡9ç¥<E¼\u00859ã\u009C\u0003U \u009B\u0018¡CåTã¹§ä\u001EÝ:\u0093Þ\u009C\u00143þðdz\u0083NÂ\u0013¸!à`\u00909Å\"\u0006¯È0\u0007Ë»¡¥\u0088G¸\u0086\\\u00829>ôäT*\u00141\u0003\u0093\u0086\u001D)R\u001EH\u0012\u0012O`:Ò°7 ×\u0089\u0017\"1Áê)Wh\u001B\\té\u0085ëNXØ\u0082¤}1Ò\u009C ã\u00928îÞôÕÐ\u0096Ç(ª¬å\u008Aóè§½9¢\n" +
                "¸A\u008C\u009EsÉÏ¥)\u0088î\u0004\u0003ï\u0081R\u0018Ô6Puääw\u00ADÝ\u0093º:H\u009F\u0003pç#\u008E)Ê¹\u001F,¸\u0018äã¥HÑ\u0086OÝç¡ç¾i\n" +
                "6Ý¥Aã\u0080;\u001A\u0085`\u001Añî%\u0095Élw¥XÛj¨b=@\u001DiÁ\u0003\u001D\u00ADÎÜ\u00058(EØ_é\u0093@\n" +
                "ÚË\u0085#\u0082¾\u0095&2»\\\u008EÜPZ?,\u0015Î;6{Ó\u0095rûs\u009C\fäÔµ¦À\u001EZ&\u0017%±\u008E+ë\u007FÙ\u001FÇ^\u0018ð\u007FÁswâo\u0016Zé+sq5²\\O'1¡rÎUG%\u0088Àükä\u0090\u000B\f/CÔ\u008A±\u001DÔí\u001AÃç\u0012\u008AÜ!n\u0007á_\u0098x¯á¦\u000BÅ^\u001A\u008EK\u008B¯*PU#7(¤äùo¢¾\u008A÷ÞÏÐ÷øw=©ÃØÙba\u0005&âÕ\u009B²Õ\u00AD\u007F\u0003ì¿\u001D~Ý\u007F\n" +
                "þ\u001Bh\u008Dá_\u0083\u001A\u0014º\u009DÂ)\"òávD_»·w5òïÄ\u000F\u008B~7ø¡âvñW\u008Bõ\u0099n®·\u0086@xH±Ð\"ôQ\\Æö\u0093£wæ\u009D\u001A\u0096\u0093\u000Eä\u0003×\u0003\u009Añ<9ð'Ãß\fa*¹f\u001DÏ\u0011$Ô«U|õ\u001D÷IÙ(§Ú)_\u00ADÍ³\u009E,Î3¶\u0095yÚ\u000B^Xè´ïÕüÏÑ-Gâ\u0095Î\u00ADð\u0092ÂûIF\u0096î[k7UN¿¼\u0089Xÿ#^Iãß\u0016]\u000Bk_\u000F4\u0085åÉ7r\u0003ÿ-d;\u009F\u009F`¨ü+âi|/ð;Kñ\u0003ÊÏw¨hÐZÙF\u0001á\u0090\u0015/ø/zäü}\u00ADÚxgÃVzî£©[\u008D\u0088E¥ \u00984÷\u000EHÜä\u000E\u0080\u009EçÒ¿ËIðn6¯\u001Bâò|¶\u008B«Qb*S\u0084b\u009Bm©µòK¾\u0089jÞ\u0088þ\u0092Áæ\u0018\\6WO\u0019\u0088\u009A\u008C\\T\u009BoºGsð/ã\u009E\u0091kûWøsà\u00941\u0099'¸Ðnn§\u0093v\u0004$\u008CD§Ý\u0082¿å__ÊCÄÁ\u0086FßÎ¿.u«Ûùu=\u001Bö\u009Að\u008C±Zkº\u0014\u008Bk©Å\u000B\u0015ß\u0012¸xÝ\u008FLðA\u001Dó_¥\u001E\u0004ñÖ\u0091ñ\u0003Á\u001Ag\u008Cô{¤x5+Eq´ýÖ#æ_b\u000EEy^/p\u000EcÀy½,&.)TQJvw\\ÿ\u0016\u008F³OOFyy~sC;Nµ'îßNöþ®[ÕnãO\tÝÜ\u0013\u0080¶\u0012å\u008F`\u0014ó^\u001DðÓâbë\u009E\n" +
                "\u0093J»¸\fÐÆê\u0099ÿdä\u007F\u009Fzïþ3øÚÏÁÿ\u0006¯îå¹U\u009Aõd´µV<³1`\u007F%É¯\u008Fü\u0013ñ\n" +
                "]!náI\u008A²O\u009E½»×Çd\u0099T±x*³kí+|·>\u0097\u0005IN\u0012GÔß¾/hZ\u0097\u00895\u007F\u0087w:¢¥ôW\u000B=\u009CNØóce\u0001\u0082ú\u0090GOzõk\u008B÷¶\u008C·\u0094d\u001FÜ\u0007æÇõ¯Ì\u009F\u0019ø×W±ñÂx\u0093HÔe\u0082æ\u0007ß\fÑ>\u0019NzäW®ø#þ\n" +
                "\u0017ñ?K²M+Å:-\u008E³´\u0084\u0013Ì\n" +
                "HG¹\u001FÎ¿¡j}\u00178«\u0089¸?\u0003Ä¼7(ÕU ÝJS\u0092\u0084£(ÊQn2~ë\u008BQ½\u009BM;î\u008F\u0082Çñ\u0096[\u0080Ïk`1iÅÅ«4\u009BNé=RÕ;ü\u008E»þ\n" +
                "\n" +
                "àÙ¼Y¡ÚxÓÃÒ$çKùn \u001CJ¨Ouê@õ¯\u008FY\u0015\u001FåSòõâ¾\u008Cø\u00ADûpßøÇH¸Ñô\u008F\u0002éPyÈÑ\u000B¹\u0003Hè\u000F\u0004\u0082kç\u0089\u000B\u0017,ùù\u0089?®kû#è\u0093\u0095ñ\u0086IáËÁæô#\n" +
                "\n" +
                "¤\u009D\u0007Ì\u009C\u009Am©Ý-9y\u0095ã+ëw¥\u0092oó_\u0012j`*giÑ\u0093u\u0014R\u009Aèº\u00AD{Ùêº\u0010\u0013°ò\tÜyÀ¥Â\u0093\u00909^\u0098\u0015*©\u0004ò\b\u0003¡â\u0081\u0016\u001Cà\u009CñÁï_ÕIj~rÞ\u0084hc,U\u0081,}(\n" +
                "»q·\u001B\u0081Á\u0006¤XÙO\u0098Ë´zã¥/\u0097½²\u008AXwÏ\u0018§Ô\u009BØ\u008CÂJ¨ØÛzpjG\u0089\u0001\u00078aÐwÅHª\u009Bw\u0005íÈÏJ\u00160ª\u0019c$¹ë»µ\u0017m\u0089Ü\u008CD¹\u0007g\u009E{S\u0084LÏ\u0087U\u001E§4ñ\u0016ôm§8ïíNE!\u0082È\u0080\u0081éCØZ\u008C\u0010\u0003\u0092Ç$þTô\u0085÷\u0002àuÍ+Æ\u000B\u0003 ÀÁÀ\u0003½\n" +
                "\u0084¸eé\u0091\u0090;Ò»°=\u0081\u0014m9S÷²¾ôäF,ÙPFxÈ©\u0016\u001C1ÛÉÇRzS\u009A3 ¸\u0003\u001C\n" +
                "\u001B³\u0015õ9\u0005GÜÄ?Ë\u008Eã¨§ì$mÀ\u0003?/4¦-\u0084\u0091/~\u0099è(Úr\u000EpsÆkk^çJw\u0018\u0098\u0003<ýA¥Ø\u0080ä\u0013\u0082x\u0003½;c\u0006fÛÏp)ðÃ\u0096Â\u008E@ïÅ\n" +
                "¤&ÈÄy\u0007`ÆF6\u0081ÏÖ\u0097c¨9\u008BpÏZ\u0099ãØø2u\u001Cf\u0092\u0018ä |ÀàñI½\u0005vD\u008A\u0099\u0004¶ÀGOj\u0091\u0018\u009EFH=\n" +
                ":\\\u0017Ù³\u0080\n" +
                "*¬\u008EÛs¸c\u001F\u0087Ö§F;¡\u0087\n" +
                "\u0095\u00199\u0018 \u000E\u0095(uE\b\u0014\u0013×4.72ºô8ÎïÒ\u009F\u0010\fÄ\u0016ç\u0007\u009E\u00944\u0082èj*\u0099\u0006ÓÏRA©QY¥\u001B{{v¦²üÛ¶ô=\u0096\u00ADÙ´0Ì%\u009C\u0012ª2õ\u001D\u0007âk\fUIQ¡*\u008A.M&ìµnÊö^oeæiN<óJö¿}\u008FLÖ¾4ß¶\u0093£x\u001BI\u0098Ei£èËm1+ó;0Ì\u0087Û\u009E\u0005y\u000E\u0081k©iÖ\u0013ZêWRÎMôí\u0013Êû\u0088\u008D¤,£'Ð\u001C~\u0015jÑo\u0083=ÍÕÆéebI^\u009C\u009E\u009FçÒ¥\u0011\u0082Øfç$õë_Ï\u009E\tx_\u008Fá\\^a\u009Fæ´ý\u009E#\u001995OFá\u0017''ÌÕýæÞ×Ñ%®¬ûþ4â\\\u001EeB\u0086_\u0083|Ôé%ymÌì\u0096\u009EI\u001D\u001FÃ\u008DB9næð~¬ê,u\u0098ü\u0089üÄÜ\u0015³\u0095`=rükÔ?g\u009F\u008FÞ/ý\u009B5ÕðW\u008A,ÌÞ\u0012»¼ØÎ¬wYI»\u001BÔ\u001E£'æ\u001E\u009FJñ+YeÓî£»\u0087ïÄáÔã¡\u0007\u008Aôo\u0088×\u0087â\u0004\u001Ao\u008B`\u009B6ÒÛ\u0018î¬`\u008B\u009F5q¸mé÷¾lû×Àý*¸\u001B\u000B\u0099eô3\u009Fgxÿ\u000E§\u0092zÂWèÓºOÍ\u0011Á9½L-YQOmRîº£Òÿkß\u008Eqx£ÆÚW\u00834+Ñ&\u009F¦[<ò¼mòÉ,À°?\u0082ãó¯\u009Elüfð_Þº¿ÊÎÉ÷5\u000E\u009D«ë\n" +
                "\u001Aè:½è\u009AM6O-\u001CãÌ\u0011\u009Fº\u001FÜ\u000E9ô®\u0012\u001DVâma!ÁÙ5Ë\u0005 ðpy¯Ã8Ï\u0082òL\u009B\u0082xvYZæ\u0084èMJIk*\u008A|ÓoÏ\u009Am[¢²Ù\u001F«ðvw[\u0013\u0098c\u0095wkI4»EÆËò¹Þß^ý²s:·%pG½D$a&H ÿ:H#u@\u008CÙ#\u009Ccõ©b\u008D\u008B2ªt9\u0004×÷×\u0086\u0099\fxw\u0080²ü½§xÓ\u008B\u0092\u0092³R\u009F¿$×\u0093\u0093Gà¼O\u0098¼Ë\u00881\u0018\u0094÷\u0093µ»-\u0013ü.7Ëf\u0019,0\u0007\u0003=¨\u008Dw&#\u0091½\u0081\u0015'\u0094[\n" +
                "\t\u001E@¥ØÁ\t^\bèM}ÌTa\u0015\u0018è\u008F\u0002MËVCåª\u009DÒ\u00022\u0007jR\u0080\u0012Â1\u009E0\u0007ó©c\u0005þS\u009C\u0003\u008Cã\u0019¡bÂ\u0097Ý\u0083\u009EÂ\u00AD;=LÆ\u0004_,nn{ñJ\u0013\u0007åî{\u001Eõ(\u008D\b\u0005º0è\u000Fë@\u000F\u0080c\u0019^ç\u0014Èãla\u0080,Ç\u00AD)ÚÇiL\u0081ÐT\u009F:°\u0007h'\u008Cc\u00AD\"ÄÛ\u007FxN7|¸\u0014î\u0084ØÄ\u0085þö\b\\\u0081Ò\u009CÑ\u0088ÀQ)À\u001F0=ªT]¡°Ã\u0004ô&\u009F·q!Ç^\u0080\u000E\u0094]Ü\u0096ÛEt\u008DÉS»\u001F\u00ADMäÉ¸ª #>´ó\u001EÅÝåàt$Ó\u0092\u001Dé¹\u0098\u0092\u001B ¤Ûaq\u009EHD\"P\u000E\u000FLõ÷¥\u000B\u0018=³\u008Er*B\"s´ýM?ËFù\u0094\u0081þÑ\u0014¹\u00AD¸\u008E>h\u00168ü´Nzp9?ZE\u0003n9Á<T¿y·;\u0003Àà\u009EÔ¯Üd\t\u0085þ\u001CVëC¥;\u0010\u0005rÙ`è=éá]\u009Bi<wâ\u009Fµ\u0081\u0001\u0094g\u0080yéNexØ¾áô¦Ý\u0083}F2\u0083\u0083»\u0091Ð\u000E\u007F\n" +
                "R®ª6\u008CdúSÂ\u0012áÄ£Ø\u000F¥\u0005\u0015T\u0089Y\u0081Û\u0083Q{\u0088nÔÎõ\u0019cÇ#\u0082iLA\u00146\u000EI§¨FQ\u0097Àì\b§\bÁ n##Ò\u008B C\u0011A$4c\u0007óÍ?doÏ\u0095\u008CpsNT8Øà1Éåiñ\u000E7»\u00021Õ»P=.4C\u001Ew\fç¯ZRU÷0V#¦}éÊ\u008A\u0014ÃëéGÊ\u0014ã9Ïw©W{\u008Aà¸\u0007¯AÇ\u0014ñ\u0001?tä\u0091Ô\u008E\u0094åEêÝ\u000F\u00AD,HÙÆç\u009C\u009E\u0094\u009D¬!\u0017j\u0010 \u008EA\u0004\u009Eõé?\u0001¥}zÛRðKÞ\bå\u009A#.\u009Fëæ\u0001ó\fö\u0005sù\n" +
                "ó¤L6Âsý*ÿ\u0087µ{ï\fë\u0096\u009Aæ\u009Cûg·\u0098:\fuÁè}\u0088þuó<]ÃØn*á¼NUY+U\u008B^\u008Fx¿\u0093³;rì\\°8ÈV]\u001E¾\u009DO$ñï\u008Eìþ\u0011~ÒÃCº¸3Y_8\u0082ù\u0016SÉn\u008D\u0093èØçë^\u0087\u001E\u008D¦«Ã$6Qí\u0084\u001F)\u0087EÏ&¸\u000Fø)ç\u0082´¿\n" +
                "øÃDø\u008B\u0005¤Ö\u0090j\u0010¤®²¯ï\ta»>ÜÖçÀ\u008F\u001EAñ\u001BáÕ\u009E´\u0084yÐ(\u0082èg\u008DÊ1\u009FÄs_Ïþ\u0003bpôéÖáìÊ\u0011\u0095JN\\ªI;J2´Ôo~ª2ÓµÏ¬âHÖ\u0084#\u008A¡&\u0093ÑÙÛGµþö\u008E´DH$\u0090H\u0018ÈêEI\n" +
                "åp\u001B\u0019äÓâ\u008B\u00046xÎ?\n" +
                "\u0095`\u0095éÏ\u001Cõ¯ê\u0016í¡ð×ÐbÈ\u001C\u00803@\u0089\u0082a\u0093óïS$qäü\u009DN\u0001§\b\u0087B3è\u000F\u00ADO5Éô*¼,\u0018g\u0007\u008E9æ\u0091\u0010\u0097*êÝ\u0089÷«\u0085Xn_/\u0007\u0007\u008A\u008AHva[Ú´\u008C\u0093V'[\u0091\u001D\u008D !\bÇ©§Æ\u008A\u009F(sÃg\u0004T\u008A\u008C1\u0093É<{RùRsó\f\u000F½\u008E´ú\u008AD\u0002 \u0006â\u000EIÀÈ©V<ð¤\u008E\u0099\u0007½<«\u0016\n" +
                "\u001F\u0018=)Ñ\u008D§\n" +
                "õ9¤Ø\u009EÄb\u0015\u008B\u0080\u009D¹ç4øc\u000FÃn$sÍJ\u0091\u0081\u008C\u0010G\\\n" +
                "~Äa´)-\u009Cý)9\u000Br%Lt\u0083×=©Ñ\u008E2¼c\u00ADHc\\mï\u009FÂ\u0095`ÛÓ\u001D\u000F^õ\n" +
                "¤\u001A\u0091ù[\u0094\u0010¸ÇRiû\u0002\u008C3~\u0095*!É}¼ý*Q\u0011`7?åI»è-Ý\u008E C\u001A\u0012väç\u009As¢ËÊ\u0007\u0004\u000E9Æ)ò\"ÆÀ\u00029\u0019>ô\u0087æ\u008D\u009F?0ã,+³G©Ó~Ã]K*\u008D c©ïHª1\u0094\u008C}ÓÔ\u009FZ\u0094BÅGÍ\u009DÜ\u008FjR¥\u001B\u0018ïÓ½\u001EH\\È\u0088FÊ2\u0006Üs\u0091Ú¤àüÎùÝÇ#\u0081J@\u0003ÌÆGN\u009D)Øé\u008C\u001E>é¤ô\u0004Õ\u0088\u0084*Ä\u0083\u0091Ær\u0017\u00AD9£b¹aÔúÔ\u008A\u0081Ï$\u009Cu\u0004Ò\u0088\u0088\u001F1\u0018Ç;OCEõ\u0013c\u0002\u0015\n" +
                "\u009B\u0080\u0003Ò\u009D¾6\u0007*\u001B'®*Q\u001A\u008C2·^\u0083m \u0088\u0095Ä\u008DÈ\u001Ft\u000Eµ-\u008D»\f+ÎÐ\u0080\u0091Ö¤\u0091~LÊª\u000E\u000F\u0004pjE]¼¢ñ\u0081Æ94»\u0018\u008DÛ@íµ\u0085+êEØÈ×¡ÎÇ\u0014í¡[n\u000FL\u0092\u0005K\u001A\u0093\u0082©Ç¡ô !-\u009Eqß4®Uô\u001Bå\u008D¿*\u0081èsN\u008DX\u0011åòÃ\u009E\u0094ð\u008AÄyn\u001BÓ\"¤\u0089Y\u0081 \u0001\u008FNþÕ2}\u0089M£Ïÿm-'Rø\u008Fð&&¹\u0096K\u008B\u009D\u0016ð\u00934Ònu\u0085Æ\u0019ì\u0019\u007FZñOØ\u007Fâ2è¾&\u0093ÀÚ\u00ADÈDÔS÷J_\u0085\u0095IÛÇbFE}c5\u008C\u001Aç\u0087õO\u0007Þ$\u001EF±dÖò¼ãýXê\u0018\u001EØ`\u000Fá_\u001Cêß\u00065\u001F\u0086\u0097IñKK¸tû6²m§\u0089\u0080È\u0091yã\u001D¸ýkù\u0083Ä*K\u0081xï\u000F\u009EQ\u008D©Õ\u0092nÛ9\u00AD&¼¹£øÝ\u009F{\u0092Ô\u0086i\u0095TÂMÝ¥oò>ÀHF\u0018e¸4õ\u0089\\\fçpé\u009FJÍð\u001F\u008A-¼oáK\u001F\u0015X¿Éu,¹å[\u001C\u008FÎ·#\u008Bsïa\u0082\u00061í_Òx<]\fv\u0016\u0018\u009A.ñ\u009AROÉ«£ájÓ\u009D\u001A\u0092§-Ó±\fQ\u000E\u00ADÿë©£O0\u0083!#iê:SÄ`\u009F\u0091p=ªX¡bw\u0001\u0080;Ö®Ö \u008CZd\u001Cg\u009FN¼Ô\u000Fmå1ó\u001C\u0013\u009FJ×\u008AÅ\u0088à1$óÍEwf\u0010\u001C°È=sJ5,ì\u000Fk\u0099i\u0011gÜqÅ*E\u0083\u0086^s\u0096lTÞPÜIný\u00058$d\u008DÊ\u000EG5§;fz²\u0015P~Ls\u009E\n" +
                ">!Æ@ÝÉ<\u008A\u00968\u0019\u008EF=8\u001D*EM¿2ãnz\u0011JîÃvèC\f\n" +
                "\u0092ÅpOL\fS\u0095X\u0011Èõ5`DdRÌ¿ð zS£·}Àlã<g½EÝõ\u0016¤\u001EZF¬ò8Îp)Ê\u008C\u001Bq8ã8ö«FÙ\n" +
                "çh'=é>Î[\u001FÓô¡I½\u0002È\u0086$-&Us\u0091\u009CÔ¢\u0005\u0019Â\u001C÷ÅK\n" +
                "»\u0015!\u0098\u0003\u009EyéVRÔc(Ý@êi6\u0083n\u0087\u009E,$\u0012¸Éï\u009AQ\u0019V(«\u0093Ôc½M°9>X\u0019\u0003\u0090iDn\u0089·n8ë\u009E+ºæÍ´îWòÁ;\u00978\u001FÂ\u0016\u0094EÔ\u009Cç=j\u007F(\u0088\u0084j§¿ÍÚ\u0080\u0085\u009C\t\u0017\u0018\u001CçÖ\u009Al\u0092!\u001EÜ\u000E6\u009EÆ\u009C¨\u0002\u0001 ^NW\u001Câ¤\u008E\u0005 3c=H¥\u0095P\u0090 \u0011\u008EF=i7¨\u0010ªàåy$ó\u009EÔäÉÁ\u0088qÜc¯½L-É\u008F!\u0097¹\u0003Ö\u0094\u0089\n" +
                "\u0082«Ø\u000E\u0005\u0017\u0002$\u0085Øna÷{\u009FJ\u0098D\u000F \u0083\u009EÀRÇ\u000F\u001DN3\u0081\u009Eõ,pFË÷\u008EGP+6Ð\u0010FAN2rGjq@À)\u001D8é\u009A\u009CA\u0082Uyäwæ¥H·\u001Cc\u0091×=sRå`+Ç\tA°7+Ó\u008E@§\u00852\u001C\u0012O¥L\u0013oF\u0003\u0003\u001C\u009E3@\u008C\u0003\u0095äç\u0083RÞ\u0082¾\u0084H¸ÀP\u000E\u001B\fqÒ¥Tù¾Piâ\u0012W\u0095ëÀÅK\n" +
                "¹L\u0097\u0003\u0003½L¥}ÆG\u001AÆGOÇ5ä¿´w\u0085®ìü5©Ía§\u0006µ¿\u008C\\\u0006\n" +
                "\u0080\u0097\n" +
                "0Ø\u0019î9Í{\u0014ppÁ\u0014\u001CôÉ£[ðö\u009Fâï\t]xg\\`bò$\u009AÌ$[\u009BÎ+\u008C}\b¯Î<Qá¿õ\u009F\u0084kP§\u001BÕ§j\u0090ïxëeê®¾g±\u0091ã>§\u008F\u008BoÝz?\u0099óçìUñ\u0012\u000B«K\u009F\u0087º\u0085Â«\u0082g´L÷\u001C0\u001F\u00905ô46åùP0Gc_\n" +
                "i÷Oð\u0093âÌ^ Ñf(°ÝnE`@8l2þY¯º<)u§ø\u009FB´ñ\u0016\u00970{{Ëu\u0092\"§±ì}Å|ß\u0083\\N³,\u0089åµ_¿CkîàöÿÀ]×Üz<Q\u0081ö8¥\u0088\u0087Ã=ý\u007Fà\u0096m´æeÎÐ8ë\u009E\u0094ô·D\u0097Ëf\u0004\u000E§¥iÅc\u001CHC\u0013Ó¨\u0015Y \"@¬\u0001\u0007¥~À§ÍsåÚ°è\u0081uÛå\u0082\tàÔ\u001A¥¤móìÁ5n$U\u001EZ§\u0003µ7P\u008F \u0010xÇ\u001B»\u001A\u0098¾Yn)=,a<D>\u0017\u001C\u001EG½:$\u0007¤k\u0091Ö¦\u009EÕ\u0081ó\u0014\u000E½qÖ\u0095\u0010*\u0002\u0017\u00078âºT\u0091\n" +
                "\t\u001DºoÈ@xÉ5(²rp\u0013\u001EÂ\u00ADØé\u0093_È¶öñ\u0012äü \u001Eø®«Jð\fVË\u001C¾%Õ¡·F8òcpò\u001FÀt¯\u001F6Ïr¬\u0092\u0083\u00AD\u008D\u00AD\u001Aqóv¿§Vtá°x\u009C\\¹hÅ³\u0094¶±ib8\u008B§R)M¸TS\u008E\u0007AÞ½KÆ·^\u0012ð·\u0087#ð×\u0086te3]Æ\u001E{«\u0095\u0006@½\u0080ô¯;ky\u0003\u0017eÇ9Á5\u009EM\u009CPÎ°k\u0017E5NZÅ½9\u0097{tO¡XÌ+ÁÕöRiÉon\u009EWêSKpÁ\u0097\u0018Ïr(kVb\u0006rIéW¢\u008E?/c®îø¨§EYw\u0001\u0082=¸¯[\u009BÞ²8Ú+GjÙà|½9«\u008BnÁ\u008C\u0002=\u0087Jtp\u0087l¨úæ\u00AD¨ò\u0090\u0002\u009BA\u0003§\u00ADDªÝ\u0002Þç\u0097ùL\u000E\t\u0005ý3N\u0010:\u0082û\u0088ùz\u0013SE\u0011ÇÊ¿\u009D8Û».\u0002\u008E:×¨Þ\u0086\u0085F·Üq\u009DØíJ\u0090F\u0018«r;\u009E\u0095d[0ùäQÇphû ÏË\u0018\u001C\u009F¥';;0+4*Ñ\u0092Wo`Gò§yIÁR\t\u0007\u009E*Y þêàz\u001Eôóo\u00826\u009C\u001Cu'\u00AD\u001CÈ\b¶ì?4d\u0010qÈâ\u0095\u0017,\u0006\u0007=Áý*]\u0084\u0092\n" +
                "à\u0091\u0081\u0091K$\u0005Îz\u0081À4^èDf\u0006f!ÉÈû£Þ\u009EF\\2Fz`ü½jT\u008Cï(H9\u0019ÀíR\u0004Ê\u0015n\u007F»Rä\u0090-H\u009Aé¿+Ó9ïO\u008E>\u0006óÏµL\u0090\u001E\u0015T°\u001DªXíðÛJã×ëY¹!«\u0015Ä(ë\u0082:\u009E\u0095?ÙW<.[Ò¬Çn ï(0F~µn\u001Bp\u0011p¸Ïñ\u000Eµ\u0094ê\u0005\u008A\"Ïn20z|Ý\u0005K\n" +
                "\u0089,@#ý£Z\u0091Ø+GÂ\u009F»Øu©?³Ù@Ø\u0083\u0004äñYJ¶\u0085Zè \u0096<\u0083±pÂ\u009E°É]\u0088\u0017\u001EÕ¨-\u0014(R\u009D\u0007\u0015\"éÙm9\u0003©\u0019¬eS]v\u000B;«\u001F\u001DþÕÿ\n" +
                " Ò<g=Å\u0085¦Øn·]Y¯M¤òëùçó®ÿö!ø\u008Fk©hoðâúm³Ûfk%cÉ\u0004üÊ=pyúW©þÐ\u001F\n" +
                "µ\u001F\u0017|8\u0097V±Ó\u0096[Í72[\u0012£s\u000Fã_~+å-\u0017T¿øWãÛ\u001F\u0014YB#Ky\u0096@\u0007\u00199ù\u0093\u008FQ\u0091_Ë9ÜêxqâDq\u0094¿Ýê¶ôÛ\u0096Oß\u008F¬^©z\u001F}\u0085\u007FÛ\u00193£?\u0089~kgó>å\u0082Ì²\u0092\u0014àq\u0093Uî,\u0095\\\u0005d\u009CàsSx/_Òüe [x\u008FJ\u009CIow\n" +
                "É\u001F|\u001Eàû\u0083Å]k_´|¢<6p\bï_ÓT1TëÂ5iÊñ\u0092M>\u008D>§ÃN\u009CâÜZÕ\u00191[yG\u001BHÇ=*¾¨>PCç\u009CçÒ»\u008F\u001Aé\u0016º\u001E\u0097§éq@>Ð-¼Ë\u0096Çvä\u000FÊ¸\u009DB&\u0005°\u0080z\u008FJ×\t\u0088\u008E&\u001EÑmýjEzn\u008Cù\u001EêÅ\u0007\u0080K\u0019f\u0007\u001Eç\u00ADK§hw:\u009DÌv\u0096q\u0016rà|µ5\u0084bá\fL07~5gNøãàM\u000Eþ\u007F\n" +
                "ø\u001D\u001Eâú(ÙuKÙaâ\u0012\u0007*\u0087ú×Îñ\u009F\u0019`83*x¬N\u00ADé\u0018¯´ÿC³,ËªfUùS²[³gÅ\u0012xgÂ\u009A;è\u001A\u001DÇüL%\u008Fý&ñÏ1\u008Eû\u007FÆ±¾\fé\u0017~(Õå6(ï§iä¼\u0093ÌÄ\u0099X\u000E\u0099ï^e¨øÂïÅ\u001E/]\u0007H\u0094Kq}>$enc\u0088\u001C\u0013\u009EÕív\u009AÆ\u0095à\u008F\t§\u0083¼%\u001BÄ¯\u0019ónCrÃ¸>æ¿\u008FhVÏ<Mâú4qU\u001Bs\u0096Ý#\u001DÝ\u0097¡÷ÿìÙV\u000ER\u008Cm\u0018¯½\u0095u]R}sY\u009Aúìîf|(\u001D\u0003\u0080=±U\u009C\u0004b\u00859Ç\u0018ª¶ó¬r\u00899Æy&®Êch÷Èäúm¯î,>\u0016\u0096\u000B\u000FN\u00855hÅ$½\u0016\u0087æ\u0015jJµIN[·r¼\u0090\u0097;\u0097©\u0019&\u0087\u0089B\u0006XÆ@çÞ£ó\u0099[fß\u0094ô$Õ¨ãY\"\u0007íê+«X«³+«\u0092év\u0002éØ\u0080pG8£TI\u0013l*Ã\bp8«zD«!\u0003\u0007\u009EÕWR\n" +
                "dÎü\u0012rÄ\u000Eõ\u008AnU\n" +
                "vP¹æñ£+l\u0018#Ô\u009A\u0091U²v\u008E3\u0082\u000Fò¨Ñ°Ø\\à\u001E\bïS+\u0010\u000F\u0019 W¯-\u000BÔ`\u0080¹Þ\u0001\u00034á\u0003(Þ\u000EÞ{\u009Eµ4*Iù\u0094óÈâ¥{fÛ\u0090\u0007¿µC\u0095·\u0002\u009F\u0095\u0096Êç\u008E¤\u000E(X\u0082à às»Ò¥x\bbIí\u008AUE\\(lä\u0002MRbÐjDCeWÜqJ\u0096á°£\u0090\u0006A&¬$^nHl\u0001O\u0010\u0081\u008F\u0093ð¬Ü\u0095ÆVHÉãó§¤\u0019mì\u0087\u008C\u008Cö©>Î\t'\u009Cg¥L!<ü£¶2x4Ü\u0085¾Ãb\u0080'ï\u0016.zç=*d\u0083\u0004\u0015\u008Cç9ÅK\u0014 ääd~µr\b\"eÚN\u000Es\u0090;V\u0012\u0095\u008AJå{{pÎ\u0006ÜãøM_\u008FM\u00057m$ö\u0015fÛO \u008DªIë»¥_KeEÜ¨Wð®iÕ×B¹JVVn\u0083\u001B½\u0087\u001DjÌvdK\u0096\u0018ÏB{Õ\u0088\u00AD÷7\t\u009E9úÔðÛ3°8Ç Ç\"°\u0095FZ\u008ABXiÐ;\u0083r\u008Cbï´g\u001Eõ$\u009AKÃ\u0087xN\u0018e\u000E8\u00AD;[wX\fA\u0088Î\u0007\u0003¨\u00ADÿ\u000E\u009Bha\u0091/´õ¼³\u0094í\u0090/XÏ¨ôþµâãó/¨·6®»uionívëÐïÂáV\"<©Ùþ^¾^}\u0004ø\u007FàÛ\u000F\u0014Y&\u009B«\\í\u0086Y\u0018mÇL\u000EOâ\n" +
                "|uû_þÎ:oÃ_\u001Fj\u009A.\u009D7\u009F¦\\Mö½&AÎ\u0014òTý\u001B#ò¯§>\"üEÕ>\u0019ø\u008FXÒt\u009D&I´«O\u000F´íxN<\u0089\u001Bîóï^\u0011ñJo\u0010x\u0087áþ\u0095yâk\u0096º¸\u0096×í\u0016nÿxå\u008F\u0019ô\"¿\u008F|Oã\u0085Ä\u0019üp¸J\u008AT\u0013oUg\u0019ù7ÝZë¹÷Ùn\u0016\u008E\u0017\u0004£(Z¢ëäº}ìÌý\u008A¾\" {\u008F\u0086:\u0084¨\u009Dg°V$6ÿãOë\u008F\u00AD}cðïÃz\u0005ç\u008A\"Ôõ¶H\u00AD¡\u0088Jð\u0096å\u0098\u000E\u0080\u001EÝëàe}OÂ\u001E'²ø\u0083á«@\u008F\u001DÀ\u0090\u0011ü2!É\u0007ë\u0082?\u001Aû\u0007Nø¥\u000FÄ?\u0016éZ¿\u0085 -¦ê\u009E\u001FI~ÔËµ¡\u0095qæÁ\u008EÅ[#é\u0083Þ¾Ç\u0082¼EÌq\u0019f\u001B\u0087©EºÒ\u009F³SmZ4Þ÷ó\u008FC\u0083\u0019\u0096a\u0095ic§ð«6»¾\u009FðN\u0093ÆRÃ®k7z¨\u008FjI#\u0018×Ñz\n" +
                "âµ[1½\u0091\u009Bòë]Uì«#2\u0086 \u009FÀb±ä±\u008EâçËdñ×©¯ê<\u001C#\u0087¥\u0018-\u0092Hø<DåZ£\u009BÝ²\u001F\u0002ø/Xñ\n" +
                "û6\u009C\u0002$9/4\u009FukçO\u0088ðê?\bdÖm¼Â·:\u008Dì¯+¯ñ31Æ=«ì«©\"ðV\u0082\u009At\n" +
                "\u0089²\u001Dó\u0010yv#½|Åâ\u007F\u0087Z\u0097Å\u007F\u0088ókz¥\u0094\u00AD§ÚKæ(ÝÄ\u008FØ\fö¯äO\u0016¸â\u001CG\u009C¬$\u0017î¨6\u0093ë'³~\u009D\u008F½ÉòÏ¨a\u0013oÞ\u0096¯Ë±Í|.X¾\u001Ah«®ksÆú¦ \u009Bæ\u0091ß\u0098bê\u0014{×Qð_âÝÏÄÝo\\i.¡\u0096\u000BIUm¼£È\u001DóT¼{ðo]ñUµÆ\u009DáÛi\u001AæHÈVCþ¬c©¨¿eO\u0083Ú·ÂÝ\u0003T]nÙc\u009Eêï§V`½ÏãZx#Aâ¸Æ8\u0084¯Ë\u00197Ù&\u00ADùÙ\u0019ñ\n" +
                "HÃ+\u009A}ZHõ%r\u0084!\u001C\u009E¸\u0015f'm\u009E[·>þ\u0095\u0019\u0084F¹ÁÉn\u009E\u0082\u009C\u00103|\u008BÓµ\u007Ff®V\u008FÍù\u009B\u001B1ýç\u009C\fp*a!TÎHõ÷¨Ú)2\n" +
                "\u009E\u008DÏ=ªT\u0013(\u0018PqëTìãbuDÖó\u0098ÀÚq\u009E´Ù\u0099ÝÎçoË4Ñ\u0082á\u0093\u0001\u0080ä\u0003Þ\u009D\u008DÃ\fH\u001D½k>T\u0099WmXà¼¶TF\u0007$\u001CÓÕT\u0010@9'Ö\u0080«\u009D\u0085¹ý3O\n" +
                "£$óÆ8®öÝµ5R%\fFÓù\u001A²\u0096Ë$AÉä÷ÏZ\u008D#\u0085\u0093-ËtÁ©áÚ\u008B\u008C\u0092ã\u0015\u008C\u009DÊO¹TÛ²¹`ÇèE,P1R\t;ºâ¬¼jOÜnO?ãSA\fy9\u0004ç¡\u001Dhr°\u0095®A\u0014\u001F.@#\u009E\u0084U¯²³íÀÏ\u00154v¦F\u001C\u0013ý*Ü\u0016,\u009C\u001E\u0084ò\u000F¥c)\u0014g}\u0090¡<s\u008EjE±\u0018\u0018Ã\u0003Ç½hý\u0080+`\u008EOAéV\u0005\u009A\t\u000B\u0081\u0080¼sÒ¡Ôv\u0004\u0093ØÍ\u0016E\n" +
                "à1ç\u0091\u009Eµ¡abKáT\u0081é\u009E\u0094è¬Á\u0097\u0085Ëc\u0082+RÊÁÂ\u009C¹'\u008Câ°©6£¹PB[Ú¤kºEÉ#\u00AD=âmÛ·g\u008A·-¶ØÂàäw5\u001B£\u0083µË\u001F¡®k\u009A1°G\u001A®vå\u008F\u0004Õ\u009Bd\u0004à\u0091\u009Cõ\u0003¥2\u001B9Y\u0082\u0086àó\u0082zV\u0096\u0099¦\n" +
                "À\u0080q\u009AÊr\u008CUÆ\u0093oBhm\u0014F\u001C\u001E\u0018\fú\u008AÉñ¦\u009Dã_\u000Fk\u001Ao\u008Eü\u000F¨:\u000BYÀÕtö\\Çulxl\u008FQÔ\u001Aë-´y&eT\f\u0001\u001DH®ÓÁ\u009E\u0017Ñ¤\u008E]K^\u008F}\u00AD¤{¥\u0084\u00907\u009EÃ&¾_\u0088e\u0081\u009ESUb¥hr¶ÚÝ[f¼×CØË)b%\u008A\u008A¥¹àÿ\u001Eü\u007Fá½oá¦»áÈ¼»K\u008Df4Tg`\u000B\u0001Ï\u0007Ò¾oøáñXëré>\u0004ð¦\u00991:-ª$\u0092È¤\u0006\u008CG»vGl\u008Aô\u000Fø)ßì¥ñ+Åº\u001C\u007F\u001A?gï\u0011¢iv7gí\u009Au\u009DÐ\u007F-OÞà\u001E\u0099¯\u0099õ\u007FÛ\u0013À¾\u0014±Õþ\u0007jv\u001Bõ%Ð~Í¦ø\u009C¦\n" +
                "ÎÑá£\u0094z\u0006È\n" +
                "_ÂRÊþ±\u0098TÄQ\u009F¶\u0092\u009C¤ÖÍ]¯y¯5©ú\n" +
                "Ju>\u0019i·Ün|.ø\u0081§|VðäþHÙçÉ$rÁ¼\u0011\fêN\u007F\u0003Æ+Õ\u007Fg\u008FÚ+MøAm©xsÇ°JtÈ¥\u0012ZI\u001CEÞÞVÂ?NJ0Á?î×çÏì\u001Fñ\u000BÄÖß\u001Cï<\u0013!W¶¼ó>Ôó\\\u0005U\u0095rC)=I9\u0018ï\u009AûGÅNlÞÓÅ\u001A,Cp\u0091DÁÔ\u0010¬½È¯¦\u009DZü!ÄÔq´b¥f¤\u0093Úý\u009F©\u0015¨sÂT'³GÖºG\u0088´¯\u0013Ú¦±£]¬öó t\u00913\u008D¤uö\u00AD_\téÖ÷Þ!\u008A;¦ýÞw±'Ó\u009Aà~\u0002x¢K»[\u001F\u00886\u009A\u0098\u0099\u001E3\u001Dæ\u009D$*±dpP¨\u001Cz\u008FÂ½¯Ã÷Þ\u0007Õ'\u009BPM<i³\\'\u0097\u001CRJ\u0004nOeÏzþ¸\u00ADÄØèp¼±ø\u008A**Tù\u0094¡%(Ý\u00AD;5¿oSàèå¸j\u0099\u0082£N¥í-c%g£ù¦fë\u009A\u001C¾0Õ\f?2Z'Þ\u0090\u001E\bô\u0015kHøo£\u0007\u0016Q!XAÁE\u0018Ýõ\u00AD²\u009AV\u0096ªÚ\u0086¥\u0015´qü\u008B\u001EðOã\u008Eµ\u0089¬|N´ÓD\u0091øv×-\u009E&\u0093¿¸\u0015ü\u00AD\u0093p'\u0011ñ>*ôh;I¶å-\"¯æ÷ù\u001Fo\u008CÌ08\b~öIynþâ/\u0012Üèß\n" +
                "\u0096[-/M\u008C\\Ü)PÍ\u001EH\u0018ë\u009Eõæí\u0002rË\u001Frzu=kKZÖ5?\u0010]5î©pò±ã-ü>Â¨Ê\u0081TªäsÞ¿®8\u0017\u0082ð\\\u0019\u0095F\u00854\u009DYk9.¯·\u0092GæùÎm<Î»{Al¿®¥a\u0013\u009F\u0098\u0013Ó\u0081éDq³\u0091\u0082pO Ô¥$A\u0092Ã\u0007¨\u000BOP\t(¤\u008E8¯½æÐñQ\u0012D\u0011pÜ\u0093Ç\u0014üð>\\r@ÏZ|q6ã÷\u0080ÏZ~ÀB±ÈÇ\u007FZ\u001C\u0095ü\u0086E\u0012¶wlÎ[¦)Â&f ñÏ÷\u0080©\u001A<9\u0019Áè0x§y\u0001Ï\u0004\fqÉ¤¤\u0094®'~\u0087\u009F\u0004ÀÀ\u008F+ýìÓÒ'\\í\u0003\u0004æ\u0097nåÚT\u001C\u009EHíR\"\u00907g$w=k·¡®¶\u001D\u001B\u009EÆsÈ\u0007\u0015d6à\u000EÓ\u00809>µ\\\u0006Ê²¶î95rÜ³'<\u009E9¨\u0092Ôwî\u0011c;\u00180;z\u009EjXÐ«\u0084\u0004óÎA§\u0005Û\u0080A'=*T\u008D\u008Bî\u0011ä\u0081Ú³m\u00ADF\u0091=¬_2ª¶2>bÜô«\u0089\n" +
                "È6\u0093\u0082=\u0005Ci\u000EXp\u0006\u0007P9\u00AD+1\u0083ó\u0081Øg\u0015ËU´Í\"®Bm¤T\u0003c\u001FoZ\u009E\u0014.\u0002\u0014ê9Í=\u0091\u009D\u0081\u0084du&¬Å\u00039\u0018\u001CñøV\u000EEZÈ\u00ADonï:¨þ\u0013ÐVÝ¬\t\u001Aã\u008C÷\u0014ËM(ÆC\u0091\u0092{â¥\u0094\u0014\u0001S\u0093Û5\u008Cä¤Ò4IÄdÑ\u0016l'#\u001C\u009Cô\u0014C\u0014r\u001C\u0090O¿\u00AD;q ©ê\u0006\b§D\u0085FÑ\u009EG\u0018©z&\u0002lÛ E^½ë¢ðæ\u009CÏ\u0087s\u009C\u008Eµ\u008Deo$³\u0005\u000B\u0091Ø\u001EÕÚøsI\u0099¢R\u0010ã\u001C\u0091\\X©¨ÇFtaé:\u0092ØÒÓlma´{§C$¨1\u001D¸?xýj\u000F\u0014xcQñO\u0087í4\u009F\u0011\u0093m\u0005Ã1\u009EÎÒB¡Ó°,9®\u0097Ã\u001AU\u009B]¨¾fÙ×\b¿{Úµ5½&úîvÔ%´òã\u000B\u0088£S÷Tt\u0015ðÙ½,\u00167\u0013\n" +
                "x\u0097xÝ;IÚ.Û+u¹õØ8Ô¥\u0085n\u0011×m\u0016¿6y%\u008Fì\u0097ð£Áw£Ä>\f}KO\u008Aå\u007FÒ\u00ADbÔ\u001Dà\u0099Hù\u0095ãbA¯\u008B?l\u007Fø%¿Ãÿ\u0012ßk:\u0087\u0081üËk½Mf¹±Ô\f¸òåÆDg±\u0007\u0018\u0015úDt«\u008B\u009D:@\u0097\u0001Ô\f´c°õ¯\"ý¤<C¡x?á£ø\u0093ZÑ£Ô¬<Á\u001DÍ\u0084S\u00018n@dï_Ìþ äñá\f×ëX\n" +
                "\u008B÷¯D´\u0092wÑ4Þ±>\u009F\u0005QWÂ{ñåk_/ë¹øA'ÁÏ\u0017|\u000F¿·ñn\u0093¨É-õ\u0095à\u0092u^«\"?#ô¯³þ\u0003|gÒ¾?x2mn\b\u0004\u0013Î|\u008DFÐ0>TÃ¸ö#\u009E\u0095\u0097ñ¿Gø\u0007ð{ÅvÚÌ?\u0012ìuSâ)¢mGÂ\u0017\u0091\u0096º²i$Êí+\u0091Æy\u0006»ëÿÙ\u0002\u000F\u0084\u001AÛüUø?§@Ú/\u0088|¹µm8nó\u00ADNÒCD\u0001ÆÜ\u009E}+Ú§\u0093æ\\cÃUëÑ\u008F4©Z[k³Û½\u00ADm/ßS\u0097\u0017\u008C¥F¤#UÙ½\u009Fèv¿³\u009F\u008C$ð\u0097\u0089æð&§ç\u0084½\u0093÷\u0005\u008FÊ²\u000E\u0080}E\\ýª5Ï\u0018Çñ£ÀsÞ_Í\u0017\u0087nîÒÞ(¡b\u0082)ûç\u001DI®\u001F^7vëkâ\u00AD\u0012p\u0093@Éç\u0086\u001C\u0087S\u0090s^\u0097û@ø+Äß´\u000Fì¼ú×Ã¨Ãëú[Åªé¨>ñ\u009A\u0013ó úó\u008F\u00AD}\u0007c¡Å\u001C\u001FW#Ä·ípÒU ¯ñE?z/ºO§\u009Aìx\u0098Ê_WÌa^\u001BT\\¯×£=\u0091\u009Dge\u008EGf\n" +
                "£nM:í1\u0019P¼t\u001CW\u0099þÉ\u009F\u001Et¿\u008F\u009F\n" +
                "-ui\u0014Á¬Ø\u0081o¬iò\u008D²Á2pr\u000FN\u0095ê²\u0088äpÍ\u009E;z×õ\u0006\u000F\u0011C\u0011\u0086§V\u0083\\\u008Dicá14êÑ\u00AD(TZ£28>a\u009EÃ\u009C¯Z\u0086uf\u0090\u0091À\u0003\u009E+Nh\u00841\u0096f \u0093ß\u00ADgÌ\u0099má\u008EIï^\u008CevrJäi\u001A\u0085Ê³d\u000EA§,#ÌÚAéØS\u0094p\u000F9'°©V&àç\u000BßÖ©ÊÄ\u0011,n§\n" +
                "3ê\n" +
                "9!<nï×Ú¬\u0098¾\\)\u001C\u001Eâ\u009A°Í'Ë\u001Ce\u009B\u001D\u0010u©r\u001F[\u0010\bÐ\u000E\t<ç\u0003µ9Ô!Ë\u001E;TâÛÊ|K\u001B)\u0007$\u0015æ\u009C×ÚN\u0095\u0018¸ÕÖ@\u008Ep¢5ÉÏZ\u0099ÖTãÍ¹téÊ¥E\u0005»<í\u0011\u0011¶¿\u001F\u0087zQ\u0019s\u009C`\u009Eõ'\u0094Ç\u001Ec\u0083\u0081Î;Òùe\u008EÐr;6+Õ(\u008E0p\u0010\u0011\u008E\u009D*Ä\u0007Ë~9Ï\\úS\"Cò\u0092½\u000FCRÜ*\u0082\töô©Ý\u008BBXÞBÿ7=²:ÕÛx\u00869~øã\u008A©\u0017Ê\u0019\u0088ëÎsWmÝ3\u0082¤ûÖ3vCWEÈ6\u009C\u0091Á#\u0095?Î´\u00AD\u0095\u0006Nw\u001EìEg[:\u0010\u001D\u0017\u0018ã¥]°GvÃ\f\u001Cã+\\u\u0015ÕÊ\u0083±u!VÈQÁþí\\³°#\u0012d\u008E{\u008A~\u009Bh¬È\\\u0081\u0083É<VÍäQX[ï8!\u0081ä×\n" +
                "J\u0096\u0097)Ó\b]36âåmr\u00AD&p:\n" +
                "©\u001CÍs!rØ ô4Û\u008B\u008FµÈÍ\n" +
                "\u008C\u000Eì*+!çLB\u0083\u0096ï\u008A¸FÊì\u0096Ûz\u0016ärN:6*å\u009D»Îÿ+\u0092HÀã¥V¶´q'ïy#\u009EEu^\u0016Ñþ×*\f\u000EOñ\n" +
                "Â½EN&´a*\u0093°ÿ\føfK\u0099\u0097\u0011\u0013Ó\u0003Þ½'LðïÙ-#W\u0088)aÈQU´-2--¼ÇAÛ\u0082;×A©\u00ADôðG<3,J\u009C\u0014\u0003$\u009Fs_\u000BÄ\u009CAO+Â<EEtº&®ý.}nU\u0096ª\u008DÇ©^\t¡Óæû\bV\fËóJWô\u0014É¢Öçblµ\u0003<+ÃÆã\u009C{{ÒÏlg\u008DZòô&Â\bcú×!ñ\u001Fâw\u0085~\u0016h\u0097¾ Ö|W\u0015µ½¼M#I$\u0081r@<\fõ&¿\u009Cs\\ë\u0011\u0099ce\u0088ÄIÙ½5ÙtGÛÒ£N\u0085\u0015\u0018ì\u008D\u001DwQÑ´\u0089\u0004º\u008E¾tå-²+²q±ý\u001B<~uðßü\u0014+â¤\u009F\u000Ed\u0083Äö\u00177Z\u008DÝÂÈ-míâ/\u0014«ÿ=\u0002/\u0019÷¯ ý¥\u007Fà§\u001F\u0017þ&]ßèþ\u0012û>\u0099¤¹1ÂÌ\u0007\u009Aê?\u008Cç\u0080kÄþ\u0016ÿÁF¾!|\u0014ø\u0086\u009A×\u008Aì\u0097Æ\u0016b?&îÏQ\u0088J<\u0092yòÉ\u0007i\u001Dx®zî¯\u0013ÕÃáqvöpzÍë%\u0017ÓÑ}û\u009CSI©{4ý\u000F*·øWû@ü`ñ\u000E«ñßOøW«L¶\u0017ës$ÒÛ6ÕØr\f2ØÇjý\u0011ý\u0090?k¿\bþÐ>\u0010\u0087Ãú¿\u0093¦ø\u008EÂ!\u0015ö\u0095/Ê[\u001CeAõô®wÃ_ðX¿ÙcVÒ6Þx?QÑÝ²$±]8\u0004çè0kæoÚ\u009Fãßì\u0093ã}tüTø\u0015ªê\u009E\u0012ñ\\/¿Í\u0082ÌÇ\u0015Éÿh\u000E\u0086¿¡r<.\u0017\u0085°ê®\u0003\u0013\n" +
                "°kÞ\u0083\u0094UÒÙÇ]Òè÷>o\u001B\n" +
                "ùÄ½\u008Dz.\n" +
                "|2ZÛ×Èú·â\u000F\u0085-¼'ã;ÿ\u000BÉ\u0001\u0016\u001A\u009CfâÑß±=Gàxüª¯ÂÿÚ\u0096Ûöj\u0086òo\u001CÚµÎ\u0087\f\u008A×@\u0092\u001E\u0013÷w.:\fv¯\u0018ø]ÿ\u0005+ðçí\u0015áÏ\u000Eü\u001Cø\u0083á\u0088 ñ¥\u008Câ;\n" +
                "vÌüº\u0082\u0001ÊH¿ÂÄr;\u008Aô\u000F\u001AxzÃQ\u0084ê\u0092[©\u008Aæ\u0006\u0082ê7@ÊOL\u0010kùÃ\u0088ñ\u007FØ|i<Ã-s£\n" +
                "\u0092r]\u001A¿Ä\u0096é\u00AD]º\u001EÖ\u001F\u000FR\u008D\u0018ÑÄZVKÑ\u009C¯ÆÏÚ/á×Â\u008F\u008EV\u007F´ÏÀmAcÒ<B£ûgNB\u00169\u009C}ì¨èÇ®kÙü\n" +
                "ÿ\u00057ý\u009A¼U\u0004Rj\u009Aäºtì\u0006è®\"ÈSõ\u0015ùÍñÛÂ+¥k²øFÆMCS¿.]\u00ADlíÛË\u0088\u001Fº\u0002\u008E\u0087\u0018¯6\u009FáÇÅ\u00AD)\u0004¯à}v1\u009F\u0095\u009EÆNGå_©ðÇ\u0016çø*\u0012\u009C&§\u0019»ÙÆËÕ$Õ¯ÖÚ=ìe\u008EÉ2ü|\u0094ª>Wê~Ëé\u007Fµçìñâc\u008D?â~\u009CÅþêÉ8Sú×Q¢øÛÂ\u001E*u\u008FCñ5\u0095Ùo¸°\\+\u0013ú×â\f2x¿H\u009C%î\u0097©[í\u0019\u001Em«\u008C\u001FÊºO\b|xñ·\u00835\u0018¯ô\u008F\u0010ÝZÉ\u000B\u0082\u0084;.\u000EkìáâvaE~ó\u000B\u0019zI¯Í3Ä\u00ADÁÔ%ü:¬ý¸û\u0005Ì*\u001A[vQ\u008E\u001Ba§Ãnì¡±ß·Zøçö0ÿ\u0082¸j\u000F\n" +
                "¯\u0080>4=½â\u0019·¿\u009B\u0004\u0081ÓiÏZû\u0083Â?\u0017¾\u000Fxø\u000B¤Ku2\u008ELR\u0001õÈ5àUúAä\u0099f+Øfø*´{J)N>º4ÿ\u0003\u0086|\u001B]ÿ\u000Eª¿f\u00ADþhÈ\u009A\u0016RHÎGOzâ>/~ÑRþÍÉ¥x±ü\u0015{«Z__\u00AD\u009C×\u0016»H´i>Ug\n" +
                "Ûq\u0003=«Ýãøkáo\u00100}\u0007Ä\u00817.H\u0090\u0086\u0003Ú¾Dÿ\u0082\u00ADøÿUý\u009F~\u0010ÇàÉ¼'\u000E®¾*óm\u001As!\tl\u0002äH\bþ,ô\u001Ct¯¯£â\u0097\u0003ñ6O[û7\u001F\u001E~W¥\u009F2oorI7®ç6\u001B\u0087s\\\u001E>\u009Fµ¥xßtÕ¾ô{eÿÅýZîÕ\u007Fá\u001C·K¦Ú#-,jìÒõa\u0091Ó\u0004âµôm+Æzþ\u009E%ñd\u0016vÎ[th\u0091\u0082GÖ¾|ÿ\u0082IüE»×~\n" +
                "é_\n" +
                "üaàýBÎïOyf¶Ö¯\"-\u001Dê¶\bË·F÷5ê?´\u009Fíoâ\u009F\u0081>)\u008FÃ¾\n" +
                "ý\u009E5ï\u0018«©2ßéª<\u0094#?.prkùó\n" +
                ">6ÄM¬\u001Cê´¯¬\\íkÛN\u0096ô½\u008F¹\u0094røÏßq¿\u009D\u008CÖÎO$.x&\u0095P\u0015ÆGÊ94ð\u0081\u0097k À=A¢%W\u0090\u008C\u000E¸ã½\u007FuÙ£ñå«\u001Bò\u0005\n" +
                "\u0007Þ\u001DV\u009E\u0091¢\u0090v\u009C\u0091ØÒÁ\u0019\u000F\u0086Ç\u000B×¥J±\u0080wð\u007F\u009Eju\u001D´\u0019\u001Ck³ËTä\u001Cc¹«¶±î\u001Cg `TPDÌC>ÝÀõZ»g\u0003É Ø@çñ¬æô\u001A¶Å\u009BK&a¼§N£Þ¶´\u00AD\u0016Y¥\u001EZ\u0013\u009Eä\u001CÒhv\u001Ei\fÝ7g\u0018ë]¯\u0086\u00AD ·F\u009Ah\u0080Çzò18\u0087\u0004uÐ¤¥+\u0015ì|-<6Ûæ`\u0006++Äw2#}\u0091¤ÈSÜVÿ\u0088|F¢/\"Øó\u008Cu®^í\u001Eé\u008C\u008D\u0082>\u0095ËGÚN\\Ó:+rGÝ\u0081\u009CÂDFPyéW´k\u001F5\u0083²à>ïz\u0093OÒeÔ®|\u0081ÀÎs\u008E\u0095×Cá¥°ÓD\u0080d°ä\u0085\u00AD+â!M%}Y\u009D\u001A3¨î\u008C\u008F±Â\u0019vãÓ9çñ®»ÂÐÚéÖm¨^#\u0094\u00857¸A\u0093\u008Fj¡á\u007F\u0005Üø\u008D\u009CÇ\"$q7Ï!85\u000Fí\u0019û@|6ý\u0092¾\u0013ÞxÓÄ\u00ADç¶Ó\u001D¥¡Á\u0092ê\\p \u007F:ø.-âü³\"ÃJ\u009Cçz¯dµwgÑäù]|DÕi«Au}}\u000F\u0002ý³¿à±¿\n" +
                "?e\u007F\u0089\u0016\u009F\n" +
                "4ï\u0002\\jÚ\u0083,R]M¼,pÆüd\u009Fï{W¼ü\u000Eý±~\u0015|uð\u0005¿\u008Bô\u009F\u0011Gh&\u0088\u001B\u009B{§\t\"\u001F¦yú×á/íEâ\u009F\u0019þÓ\u001F\u001Cu_\u0089·ÖÆÙµ+¢ÐC»\u0088bÎ\u0015\u0007°\u0015ößìS¡x¿Rø\n" +
                "c ü8øu\u007Fªøº9<©ï'\u008C\u008BH\u0093¢¶z\u0013\u008AþtÏ1Ù\u0086:\u0095)Nr«ROáNé7Ù-\u008Fº\u0084©áÕãd\u008Fº>4~Ùÿ\u0003>\u0011xuõ\n" +
                "OÅq_^\u0098ØYéöïºY\u001B·\u001D\u0086kó\u007Fâ'\u008Bþ;~Õ\u007F\u0010®E\u0094:µÜW\u0097e\u00ADôÛròE\n" +
                "\u0093À\u0003¥}cð{þ\t\u0085\n" +
                "¶\u00AD7\u008D~>kÍ«êW²y\u0092YÆÇld\u009Cà\u001Côí\u008Aû\u0003àßÁ\u009F\u0087¾Ó£²ð\u008F\u0084l¬B\u008Co\u008A\u0018\u008Fvë^ÎWá¾u\u008BµlcT£ç¬¾î\u009F3Ï¯\u0099S«S\u0096:\u009F\u009D?\f\u007Fà\u008F\u007F\u0015¼g\u0004z·Ä;«}\u001AÑÀ/ç|Ò\u0090{mí^Ûàïø%7ì½à+5\u0097XÑæÖ.\u0011~õÙÂdz(¯´|[ ¶\u0089l\u0090\u008EzsÍpþ+\u0012ClÑ¯~\u009C×ê\u0099\u0007\u0002d8\u000EW(:\u0092ï-\u007F\n" +
                "\u008F\u001B0ÇW\u008F2\u008B±óÔß²¿ìé£7\u0097að\u0083C\u001E\u00ADd§ù\u008AÈñ/ì»û8kV\u00ADo©|\u0019Ð¥Ü§\u0018±PsíÅzÎ§nÍ\u0096#\u0083\u009CgëYw¶\u008AÈK`\u008Cdb¿R£\u0096åÑ¦¢©Fßá_ä|eLf/\u009B\u0099MßÕ\u009F4j\u007FðM\u008FÙVçÄ\u0016¾+Ñ¼\ft\u009Bû+\u0095\u009E\tì$+±Ôî\u0007\u001D:ö¬Ï\u0014øz}\u0007Ä:·\u0080µ\u001DÆ=\u009Ee\u0084\u008E¼È§\u0090Ãõ\u001F\u0085},ñ\u0005\u0090\u0091\u009C\u001FÎ¼»ö¢ð\u009AËá«o\u001DXBÿiÓ&òç1¯>K÷>Àãó¯Ì¼Yà|\u000EmÂõ18J*5¨ûé¥k¥ºûµù\u001E¦I\u009Cb\u009664ëMÉ=5{3\u0003ö'Ó|\u0017\u00ADxÓUðß\u0088ü7e&²©çZÝMn¦GAÁ@Häô?\u009D}%â_\fè)iöY¼?fÅxÃ[)þ\u0095ñ?\u0087|U}ð¯Çz'Ä\n" +
                "\u000EBò\t\u0096V*888e9ìA\"¾à»×4¿\u0013é6Þ!Òæ\u0012[Þ[,°º\u009E¡\u0086kÊð_?£\u0099ä_Q¨\u0097=\u0017åw\u0016ô\u007F'u÷\u001D¼G\u0087\u0095*\u008A¼[÷¿\u0007ÿ\u0004àõ?\u0086¿\u000F52Ë¨ø#J¸ãæ2X¡Çé\\W\u0089\u007Fd\u000FÙ·Å±°Ö>\u0010èìO%£´\bGå^¥/\u000FÁ\u0003\u001DG¯Z\u008D\u00901±\u0004õ5ûlð8:ÊÕ)Åú¤Ï\u0092X¬M'îÍ¯\u009B>qñ'ü\u0012ÃöSñ\u001EçÓ<7s¥K\u008C¬Ö\u0017D`úà×8ÿðO\u000F\u008A¿\u000Fn|ï\u0083_´=äQ\u0082\fvº¤eÀ\u0003¶A¯®\u00ADíä\u0089<Â\u00063Õ©\u0097,\u001DñÉ qº¾w2à~\u0014Î\"á\u0088ÂÁ¯%oÈî¥\u009EftWñ\u001BõÔñÍCâÇÇ¿Ù£ÃqÉ\u001F\u0080ï¼m<:pYîìBª¬ÄòÁ~ö\u0014~f¸\u001F\b~Õ\u007F\u0001þ2C\u007Fà/ÚY/Üê7bo±x\u009E\u0002\u0086ÕÇhÙºsé_O¢\u0096å\u0087Þ\u001C\u008FZç|oðWáwÄ«cmão\u0003i×Û\u0081\u0006I-\u0094:û\u0086\u001C×äùßÑó#\u00AD\u0087\u0094r¬Dè7¯}}U\u0099îá¸¾ºiW\u0085ÒíþLõ_\u0085~)ø\u0019â\u000F\fYhÞ\u0014µÓ~Ãmn\u0091[\u00AD\u0096Á\u0085\u0003\u0019ùk¡\u009Bà\u0086\u0081¬Lnô=sË\u008D\u0086vHs·Ú¾1¾ý\u0087[Á\u0097rk\u009F³×Å\n" +
                "OÃwY,\u0096rÌÒÛ\u0093þé9\u0015kLý¢¿o?\u0081àø\u007FÅ\u009F\n" +
                "×ÅQcm¾¥£É\u009Døþð=8¯Ê\u007FÕO\u001E<2ºÉñ2ÄaÖ\u008A/ß_(½WÊÇ½\u001CË\u0086ó¯÷\u0098.oü\u0005\u009D¢ Ü\u0017·ÓÒ¥X¢Àùyló\u009E\u0094Q_ÝÍ³ó2D·@¬X\u0093\u0083\u0081\u009Fza\u0084#\u0004\u0007éE\u00151l\u009E¥\u008BT\u000F0\u0089º\u001Es]&\u0091 [M\"+9ã\u00078¢\u008AãÅÊQ\u008E\u0086´RrÔï|9á«\u0016¶óX\u009C\u0081Ó\u0014x\u0097R]6Õ¬¡µS\u00900ùÆ9Å\u0014WËÖ©78Ýõ_\u009A=º\u0011\u008F³Û£ü\u008EF)äº¸Úç\u009EßçÚ®I\u0084ýÚ\u008C\fÿJ(¯j{\u009E\\[q4´y\f\f\u00865÷8ë\\§Å?\u008B~'³øÍ¡|3ÒÙ-íYVi¦\u001C´\u0099\u0003*GLQE~Iâ¾?\u0019\u0097ä*xi¸IÉj·>\u009F\u0087£\u0017Yß±íV\u009A\u008BiúTËo\u0002(TÜÛ8,pkò{öÿý¢¼oñÓã\u0005Æ\u0083¯l·Ó´k\u0087·±²\u008D\u008B*ã«\u009E\u0099cE\u0015üá<n/\u001FYÖÄMÎVÝ»³î*¥J\u0094c\n" +
                "\u0011ê?°÷ì\u001Fð¯â\u0006\u009Foã\u009F\u001CÝÍ¨\u001D\u0081þÈa\b\u0084\u009FS\u0093\u009Aý\u0019øIàÿ\n" +
                "x#Ã\u0089¡xSÃö¶6Ñ«\u001D¼Ar\u0007®:ÑE\u007FEpVW\u0097ÑáèbaM{Io.¿{>_\u0013^«Æò_C{ìPÜ^\n" +
                "ê:úz×eá½2Ùm7\u0005\u0019Tã\u008A(¯¢ÆJ\\\u0089\u001D\u0018HÅÔ½\u008E7ÅÓHu\u0081\u0096èp+\u008Eñl¬-ß \u0013Î\tüh¢½\u008C\u0012W\u0089âãÛ´\u008E6é\u0084\u008Abd\u001F{\u00ADaßÌèÛTðÝ¨¢¾\u0092\u0081òµ[å3ó¹²À\u0010;zÒ\\xvÃÅzdÚ\u0006¦\u009B\u00ADïahåR3Á¢\u008Aé«\bT¦ã%tô\u007F3\u001AzTM\u001F\u001D\u001D6\u001B\u008D?TÐæ9\u001AeÜÉ\f\u0083\u008C\u0094\u0090¨8íÀ¯¥¿eÍ\u007FVÕþ\u0019\u001D+TºóF\u009Fpc·}¸Â\u0015Ý·\u001EÜþtQ_ÇÞ\u0016NXO\u0014+aè¾X~õ[¥\u0096©~\bý\u001F:Jy5å¿»ù\u009DÌ\u0081\u0001$/jX I&\u0019ëëE\u0015ý\u0082¾\u0013ó{.rýÛ\b¬ÕQG#¸¬äÃ¸\u0018Á#$\u008EôQYÒmÂâ©ñ\u0093C\u001AH\u008D.1\u0081Ð\u001Atq\u0085Ã\fvÈ\u0003\u00AD\u0014V\u0088\u0089\u0012¬!>br[Û¥Z\u008EÙW8=ý(¢³¨ß+.\u0007ÿÙ";
    }

    private String getContentTypeFromResponse(String response) {
        return "jpeg";
    }

    private String sendHTTPRequestToServer(String request) {
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName("http://ceit.aut.ac.ir");
            Socket socket = null;
            try {
                socket = new Socket(addr, 80);
                DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outToServer.writeBytes(request);
                // Receive an HTTP reply from the web server
                boolean loop = true;
                StringBuilder sb = new StringBuilder();
                while (loop) {
                    if (inFromServer.ready()) {
                        int i = 0;
                        while (i != -1) {
                            i = inFromServer.read();
                            sb.append((char) i);
                        }
                        loop = false;
                    }
                }
                socket.close();
                return sb.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // send an HTTP request to the web server

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return null;
    }
}
