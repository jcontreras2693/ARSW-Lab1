/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arsw.blacklistvalidator;

import edu.eci.arsw.spamkeywordsdatasource.HostBlacklistsDataSourceFacade;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hcadavid
 */
public class HostBlackListsValidator {

    private static final int BLACK_LIST_ALARM_COUNT=5;

    /**
     * Check the given host's IP address in all the available black lists,
     * and report it as NOT Trustworthy when such IP was reported in at least
     * BLACK_LIST_ALARM_COUNT lists, or as Trustworthy in any other case.
     * The search is not exhaustive: When the number of occurrences is equal to
     * BLACK_LIST_ALARM_COUNT, the search is finished, the host reported as
     * NOT Trustworthy, and the list of the five blacklists returned.
     * @param ipaddress suspicious host's IP address.
     * @return  Blacklists numbers where the given host's IP address was found.
     */
    public List<Integer> checkHost(String ipaddress){

        LinkedList<Integer> blackListOcurrences=new LinkedList<>();

        int ocurrencesCount=0;

        HostBlacklistsDataSourceFacade skds = HostBlacklistsDataSourceFacade.getInstance();

        int checkedListsCount=0;

        for (int i=0;i < skds.getRegisteredServersCount() && ocurrencesCount<BLACK_LIST_ALARM_COUNT;i++){
            checkedListsCount++;

            if (skds.isInBlackListServer(i, ipaddress)){

                blackListOcurrences.add(i);

                ocurrencesCount++;
            }
        }

        if (ocurrencesCount>=BLACK_LIST_ALARM_COUNT){
            skds.reportAsNotTrustworthy(ipaddress);
        }
        else{
            skds.reportAsTrustworthy(ipaddress);
        }

        LOG.log(Level.INFO, "Checked Black Lists:{0} of {1}", new Object[]{checkedListsCount, skds.getRegisteredServersCount()});

        return blackListOcurrences;
    }

    /*
    Se hace una sobrecarga de la función, por comodidad y para no deshacernos del anterior código.
    */
    public List<Integer> checkHost(String ipaddress, int numThreads){
        ArrayList<BlackListThread> threads = new ArrayList<BlackListThread>();
        ArrayList<Integer> blackListOcurrences= new ArrayList<Integer>();
        HostBlacklistsDataSourceFacade skds = HostBlacklistsDataSourceFacade.getInstance();
        int serverCount = skds.getRegisteredServersCount();
        int inicio = 0;
        int delta = serverCount / numThreads;
        int fin = delta;
        /*
        El proceso cambia un poco dependiendo de que la cantidad de hilos sea par o impar.
         */
        if (numThreads % 2 == 1){
            for (int i = 0; i < numThreads - 1; i++){
                threads.add(new BlackListThread(inicio, fin, ipaddress));
                threads.get(i).start();
                inicio = fin;
                fin += delta;
            }
            fin += serverCount - fin;
            threads.add(new BlackListThread(inicio, fin, ipaddress));
            threads.get(numThreads - 1).start();
        }else{
            for (int i = 0; i < numThreads; i++){
                threads.add(new BlackListThread(inicio, fin, ipaddress));
                threads.get(i).start();
                inicio = fin;
                fin += delta;
            }
        }
        int checkedListsCount = 0;
        for (int i = 0; i < numThreads; i++) {
            BlackListThread obj = threads.get(i);
            checkedListsCount += obj.getCheckedListsCount();
            try{
                obj.join();
                blackListOcurrences.addAll(obj.getBlackListOcurrences());
                /*
               En caso de que se compruebe que la IP no es confiable se termina el proceso.
                 */
                if (blackListOcurrences.size()>=BLACK_LIST_ALARM_COUNT){
                    skds.reportAsNotTrustworthy(ipaddress);
                    LOG.log(Level.INFO, "Checked Black Lists:{0} of {1}", new Object[]{checkedListsCount, skds.getRegisteredServersCount()});
                    return blackListOcurrences;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
            skds.reportAsTrustworthy(ipaddress);

        LOG.log(Level.INFO, "Checked Black Lists:{0} of {1}", new Object[]{checkedListsCount, skds.getRegisteredServersCount()});

        return blackListOcurrences;
    }

    private static final Logger LOG = Logger.getLogger(HostBlackListsValidator.class.getName());

}
