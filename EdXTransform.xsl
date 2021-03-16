<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:bib="http://bibtexml.sf.net/"
    xmlns:wb="http://oli.web.cmu.edu/activity/workbook/"
    exclude-result-prefixes="xs"
    version="2.0">
    <xsl:output
        method="xml" 
        doctype-public="-//Carnegie Mellon University//DTD Workbook Page 3.8//EN"
        doctype-system="http://oli.web.cmu.edu/dtd/oli_workbook_page_3_8.dtd"
        indent="yes"/>
    
    <!--Setting up to find the LO file-->
    <xsl:variable name="fileid" select="workbook_page/@id"/>
    <xsl:variable name="lofile" select="concat('LOs_',$fileid,'.xml')"/>
    <!--File that maps the EdX file names to the new OLI file names. 
        At the moment must be in the same directory as xsl file.-->
    <xsl:variable name="pagestable" select="'pagesTable.xml'"/>
    <!--For courses with videos, a file I make with the videos xml 
        At the moment must be in the same directory as xsl file.-->
    <xsl:variable name="videosfile" select="'allVideos.xml'"/>
    <xsl:variable name="transcriptPrefix" select="'transcript'"/> <!--a prefix I add to pages made from srt transcript files to make them more recognizable. Up here for easier changing if I decide to-->
    
    <!--I don't want to miss anything, so adding the identity transformation to by default copy everything
        (and be overidden by more specific templates for things that need different handling)-->
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
    
    <!--pulling in the learning objectives from their file. 
        Note, atm LO file needs to be in the same directory as the xsl document.
        https://www.xml.com/pub/a/2002/03/06/xslt.html-->
    <xsl:template match="workbook_page/head">
        <head>
            <xsl:apply-templates select="@* | node()"/>
            <xsl:if test="document($lofile)">
                <xsl:apply-templates select="document($lofile)/objectives/objective"/>
            </xsl:if>  
        </head>
    </xsl:template>
    <xsl:template match="objective">
        <objref>
            <xsl:attribute name="idref">
                <xsl:value-of select="./@id"/>
            </xsl:attribute>
        </objref>
    </xsl:template>
    
    <!--were-h1s will be put in comments - don't want them in the output because the page title was already generated from display name, 
        but want them available because the two don't always match and this way they are easily available to compare-->
    <xsl:template match="h1">
        <xsl:text disable-output-escaping="yes">&lt;!--</xsl:text><h1><xsl:value-of select="."/></h1>--<xsl:text disable-output-escaping="yes">&gt;</xsl:text> <!--Trying to figure out how to do this. &lt;!- etc generates that same thing in output, which I don't want. From https://stackoverflow.com/questions/3932152/output-element-in-comments tried the xsl:comment thing but that gets rid of the h1 tags. From same source found this, which works!--> 
    </xsl:template>
    <!--generated-from-htmls-sections headed by h1s should actually not be sections, since the h1 is just the page title, not a section on the page-->
    <xsl:template match="section[h1]">
        <xsl:apply-templates select="@* | node()"/>
    </xsl:template>
    
    <!--sections were already generated so just need to change headers to the titles of those sections-->
    <!--MAY WANT TO REVISE TO ACCOUNT FOR POSSIBILITY OF MULTIPLE TITLES IN A SECTION-->
    <xsl:template match="h2|h3|h4|h5|h6">
        <title><xsl:value-of select="."/></title>
    </xsl:template>
    <!--and put in bodies after the titles-->
    <xsl:template match="section[h2|h3|h4|h5|h6]">
        <section>
            <xsl:apply-templates select="h2|h3|h4|h5|h6"/>
            <body>
                <xsl:apply-templates select="*[not(self::h2 or self::h3 or self::h4 or self::h5 or self::h6)]"/>
            </body>
        </section> 
    </xsl:template>
    
    <!--there's at least one page where the first part has both an h1 and an h2 for unclear reasons. This is to handle that case-->
        <!--section is treated like an h1 section; that is it should not be a section-->
    <xsl:template match="section[h1 and h2]" priority="1">
        <xsl:apply-templates select="@* | node()"/>
    </xsl:template>
        <!--the h2 is commented out like the h1-->
    <xsl:template match="section[h1 and h2]/h2" priority="1">
        <xsl:text disable-output-escaping="yes">&lt;!--</xsl:text><h2><xsl:value-of select="."/></h2>--<xsl:text disable-output-escaping="yes">&gt;</xsl:text> 
    </xsl:template>
    <!--there's also at least one page where the source had empty headers, leading to sections with empty titles and bodies by default processing, which is invalid. 
        Instead they should be removed.-->
    <xsl:template match="section[count(*)=1 and h2[matches(text(),'\s*')]|h3[matches(text(),'\s*')]|h4[matches(text(),'\s*')]|h5[matches(text(),'\s*')]|h6[matches(text(),'\s*')]]" priority="1"></xsl:template> <!--'has one child (so only the header) and the header is either empty or has just whitespace (in case that comes up)-->
    
    <!--Get rid of the old LO section, since LOs get put in differently-->
    <xsl:template match="div[descendant::li[matches(text(),'LO WAS HERE')]]"></xsl:template>
    
    <!--don't think we need these since we do our own styling-->
    <xsl:template match="link[@rel='stylesheet']"></xsl:template>
    <xsl:template match="@style"></xsl:template>
    <!--these seem to generally have their contents commented out already?-->
    <xsl:template match="style"></xsl:template>
    <!--colgroups (in tables) are only for styling (if I understand correctly), which we don't allow, so removing-->
    <xsl:template match="colgroup"></xsl:template>
    
    <!--bold and italics-->
    <xsl:template match="strong | b">
        <em style="bold"><xsl:apply-templates select="@* | node()"/></em>
    </xsl:template>
    <xsl:template match="i | em"> <!--POSSIBLY CHECK note, we also allow a plain em tag but it looks like it produces bold italics and html em is usually italics?-->
        <em style="italic"><xsl:apply-templates select="@* | node()"/></em>
    </xsl:template>
    
    <!--blockquote-->
    <xsl:template match="blockquote">
        <quote style="block"><xsl:apply-templates select="@* | node()"/></quote>
    </xsl:template>
    
    <!--links that jump to other course pages (use the pagestable to find the correct oli id of the page)-->
    <xsl:template match="a[matches(@href,'/jump_to_id/[a-z0-9]+')]">
        <xsl:variable name="pagetarget" select="tokenize(./@href,'/')[last()]"/>
        <xsl:variable name="olipageid" select="document($pagestable)/pages/page[@filename=$pagetarget]/id/@pageid"/> <!--in a variable since I'll use it twice-->
        <activity_link>
            <xsl:attribute name="idref">
                <xsl:value-of select="$olipageid"/>
            </xsl:attribute>
            <xsl:attribute name="title"> <!--for accessibility-->
                <xsl:value-of select="$olipageid"/>
            </xsl:attribute>
            <xsl:apply-templates select="@*[name()!='href' and name()!='title'] | node()"/> <!--the internal links I saw didn't have titles but in case some do-->
        </activity_link>
    </xsl:template>
    <!--external links-->
    <!--<xsl:template match="a">
        <link>
            --><!--<xsl:if test="./@target='[object Object]'"> 
                <xsl:attribute name="target"><xsl:text>new</xsl:text></xsl:attribute>
            </xsl:if> 
            <xsl:apply-templates select="@*[name()!='target'] | node()"/>--><!--
            <xsl:attribute name="href">
                <xsl:value-of select="./@href"/>
            </xsl:attribute>
            <xsl:if test="./@title"> 
                <xsl:attribute name="title"><xsl:value-of select="./@title"/></xsl:attribute> 
            </xsl:if> 
            <xsl:if test="./@target='[object Object]'">
                <xsl:attribute name="target"><xsl:text>new</xsl:text></xsl:attribute> 
            </xsl:if> 
            <xsl:apply-templates select="@* | node()"/>
        </link>
    </xsl:template>-->
    <!--later: potentially what was wrong here is that to put just text in an attibute you don't use xsl:text, you just write it in? See notes.-->
    <xsl:template match="a[not(@href)]"> <!--kth ramp i has some a's without hrefs that if they go through produce links without hrefs which is not allowed, so, stripping.-->
        <xsl:apply-templates/>
    </xsl:template>
    <!--kth ramp i also has some links that link to pdfs within the course directory with relative links. Handling those-->
    <xsl:template match="a[matches(@href,'/static/\S+')]">
        <link>
            <xsl:attribute name="href"><xsl:value-of select="concat('..',replace(./@href,'static','webcontent'))"/></xsl:attribute>
            <xsl:attribute name="target"><xsl:value-of select="'new'"/></xsl:attribute> <!--should prob open in a new window-->
            <xsl:apply-templates select="@title | node()"/> <!--the ones I saw didn't have a title but if one did then good to keep-->
        </link>
    </xsl:template>
    
    <!--inline assessment links. Trying to do them in this converter (from the format they currently come out of the java in).-->
    <xsl:template match="codeblock[@syntax='xml' and matches(normalize-space(text()[2]),'^&lt;problem\surl_name=&quot;[0-9a-z]+&quot;/&gt;$', 's')]"> <!--right now they're in codeblocks; this looks for the right codeblocks with the right contents-->
        <xsl:variable name="activityid" select="./tokenize(text()[2],'&quot;')[2]"/> <!--get the activity id, format it right, make the assessment link-->
        <wb:inline idref="{concat('a_',$activityid)}"/>
    </xsl:template>
    <!--video links. Looking at kth ramp i-->
    <xsl:template match="codeblock[@syntax='xml' and matches(normalize-space(text()[2]),'^&lt;video\surl_name=&quot;[0-9a-z]+&quot;/&gt;$', 's')]"> <!--right now they're in codeblocks; this looks for the right codeblocks with the right contents-->
        <xsl:variable name="videoid" select="./tokenize(text()[2],'&quot;')[2]"/> <!--get the video id-->
        <xsl:variable name="videosection" select="document($videosfile)/videos/video[@url_name=$videoid]"/> <!--getting the relevant video's part in the file-->
        <!--if there's a youtube video, use that, otherwise link to the download links provided-->
        <xsl:choose>
            <xsl:when test="$videosection[(@youtube_id_1_0 and not(@youtube_id_1_0='')) or @youtube]"> <!--currently videos seem to have both if they have one, but not assuming (but the 1_0 can be empty, in which case it doesn't count)-->
                <youtube>
                    <!--getting the youtube address-->
                    <xsl:choose>
                        <xsl:when test="$videosection[@youtube_id_1_0 and not(@youtube_id_1_0='')]">
                            <xsl:attribute name="src"><xsl:value-of select="$videosection/@youtube_id_1_0"/></xsl:attribute>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:attribute name="src"><xsl:value-of select="tokenize($videosection/@youtube,':')[2]"/></xsl:attribute> <!--if I understand right how this attribute works-->
                        </xsl:otherwise>
                    </xsl:choose>
                    <xsl:if test="$videosection/@display_name and not($videosection/@display_name='')"> <!--if there's a nonempty display name make it the title-->
                        <title><xsl:value-of select="$videosection/@display_name"/></title>
                    </xsl:if>
                    <xsl:if test="$videosection/transcript"> <!--if there's a transcript, link to it and the page made from it-->
                        <caption><activity_link target="new" idref="{concat($transcriptPrefix,tokenize($videosection/transcript/@src,'.srt')[1])}">View transcript</activity_link><!--, <link href="{concat('../webcontent/',$videosection/transcript/@src)}">Download transcript</link>--></caption> <!--transcript downloading wasn't working, and I decided that since the page version exists now just using that makes more sense anyway-->
                        <alternate idref="{concat($transcriptPrefix,tokenize($videosection/transcript/@src,'.srt')[1])}">View transcript</alternate>
                    </xsl:if>
                </youtube>
            </xsl:when>
            <xsl:otherwise> <!--videos with no youtube address are currently made into lists of the download links-->
                <ul style="none">
                    <xsl:if test="$videosection/@display_name and not($videosection/@display_name='')"> <!--if there's a nonempty display name make it the title-->
                        <title><xsl:value-of select="$videosection/@display_name"/></title>
                    </xsl:if>
                    <li>Download:
                        <ul style="none">
                            <xsl:for-each select="$videosection/video_asset/encoded_video[not(@profile='youtube')]">
                                <li><link href="{./@url}"><xsl:value-of select="./@profile"/></link></li>
                            </xsl:for-each>
                        </ul>
                    </li>
                    <xsl:if test="$videosection/transcript"> <!--if there's a transcript, link to it and the page made from it-->
                        <li>Transcript:
                            <ul>
                                <li><activity_link target="new" idref="{concat($transcriptPrefix,tokenize($videosection/transcript/@src,'.srt')[1])}">View transcript</activity_link></li>
                                <!--<li><link href="{concat('../webcontent/',$videosection/transcript/@src)}">Download transcript</link></li>-->
                            </ul>
                        </li>
                    </xsl:if>
                </ul>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!--bibliography--> <!--now should recognize Swedish version also (hence accepting 'Referense'). Swedish cs101 also used h3 rather than h2-->
        <!--the bottom-of-page part-->
            <!--the reference section should be turned into the bib file-->
    <xsl:template match="section[(h2|h3)[matches(text(),'Referen[cs]e')]]" priority="1"><!--priority is so there is no ambiguous match with the general section handler-->
        <bib:file>
            <xsl:apply-templates select="./descendant::li | p"/> <!--kth swedish cs101 has these in p's-->
        </bib:file>
    </xsl:template>
            <!--each work cited should be a bib entry-->
    <xsl:template match="section[(h2|h3)[matches(text(),'Referen[cs]e')]]/p"> <!--kth swedish cs101 has works cited in p's, and doesn't have links in them thus far-->
        <bib:entry id="{concat('p',count(./preceding-sibling::*))}"> <!--since no id is provided, making one-->
            <bib:misc>
                <bib:note>
                    <xsl:apply-templates/>
                </bib:note>
            </bib:misc>
        </bib:entry>
    </xsl:template>
    <xsl:template match="section[(h2|h3)[matches(text(),'Referen[cs]e')]]/p//*"> <!--want the text and nothing else-->
        <xsl:apply-templates/>
    </xsl:template>
    
                <!--there are at least two ways works can be formatted wrt where their ids are. Processing both.--> 
    <xsl:template match="section[(h2|h3)[matches(text(),'Referen[cs]e')]]//li[a[@name]]" priority="1"> <!--NEEDS IMPROVEMENT. how do I say first child?-->
        <bib:entry>
            <xsl:attribute name="id"><xsl:value-of select="./a[1]/@name"/></xsl:attribute>
            <!--not currently trying to sort out the pieces/format of the works cited, so they're all misc->note-->
            <bib:misc>
                <bib:note>
                    <xsl:apply-templates select="@* | node()"/>
                </bib:note>
            </bib:misc>
        </bib:entry>
    </xsl:template>
    <xsl:template match="section[(h2|h3)[matches(text(),'Referen[cs]e')]]//li[@id]" priority="1">
        <bib:entry>
            <xsl:attribute name="id"><xsl:value-of select="./@id"/></xsl:attribute>
            <!--not currently trying to sort out the pieces/format of the works cited, so they're all misc->note-->
            <bib:misc>
                <bib:note>
                    <xsl:apply-templates select="@*[name()!='id'] | node()"/>
                </bib:note>
            </bib:misc>
        </bib:entry>
    </xsl:template>
            <!--handling contents of the works cited li's:
               -we want to delete the Return to text above span and its contents, since that's the 'return to text above' piece and we don't want it
               -sometimes there's just an a instead of the above span. We still want to get rid of it.
               -we want to delete the a that held the id in its @name, since that was basically just holding the id and we handled it already. But we can't say first a since only some items have it. Checking for the name attribute the a being empty
               -other a's should have their href content also printed in case the url was not included in text
               -all other descendants we want the bottom-out text of and nothing else
               (the specific ones have higher priority than the general descendent one so they fire when needed)-->
    <xsl:template match="section[(h2|h3)[matches(text(),'Referen[cs]e')]]//li/span[@title='Return to text above']" priority="1"/>
    <xsl:template match="section[(h2|h3)[matches(text(),'Referen[cs]e')]]//li//a[@alt='Return to text above']" priority="1"/>
    <xsl:template match="section[(h2|h3)[matches(text(),'Referen[cs]e')]]//li/a[@name and count(*)=0]" priority="1"/>
    <xsl:template match="section[(h2|h3)[matches(text(),'Referen[cs]e')]]//li/a" priority=".9">
        <xsl:apply-templates/>
        <xsl:text> [</xsl:text><xsl:value-of select="./@href"/><xsl:text>]</xsl:text>
    </xsl:template>
    <xsl:template match="section[(h2|h3)[matches(text(),'Referen[cs]e')]]//li//*">
        <xsl:apply-templates/>
    </xsl:template>
        <!--in-text citations-->
    <xsl:template match ="sup[span[a[@alt='Click to view full citation']]]"> <!--need a way to catch the citations. Need to check if this works enough.-->
        <cite>
            <xsl:attribute name="entry"><xsl:value-of select="substring(./span/a/@href,2)"/></xsl:attribute> <!--need to strip out the #-->
            <xsl:attribute name="title"><xsl:value-of select="./span/@title"/></xsl:attribute> <!--keeping it since it's there and helps accessibility-->
        </cite>
    </xsl:template>  
    
    <!--not fully complete (though more than quickpass)-->
    <xsl:template match="audio">
        <audio>
            <xsl:attribute name="src">
                <xsl:value-of select="concat('..',replace(./@src,'static','webcontent'))"/>
            </xsl:attribute>
<!--TO HANDLE BETTER LATER atm this just assumes all audio is in mp3 format, which is clearly incorrect and it should properly check them. (However, need to know what to do if I find a none of the above).-->
            <xsl:attribute name="type">audio/mp3</xsl:attribute>
            <!--their controls element looks like it could be fairly simply turned into ours, but since ours is optional I am postponing this for the moment-->
            <caption>
                <xsl:apply-templates/> <!--in the philanthropy course, the audio elements tend to contain text saying "Your browser does not support the audio element.". We don't allow text directly in an audio element, so I need to pick an allowed element to put it in. I have picked caption out of the options, even though it doesn't actually seem to make sense for the text in question, because I'm unsure what might be better-->
            </caption>
        </audio>
    </xsl:template>
    
<!--Some quickpass things - TO ACTUALLY HANDLE LATER-->
    <xsl:template match="*[local-name()='math']"> <!--handling namespace problem: https://stackoverflow.com/questions/5239685/xml-namespace-breaking-my-xpath-->
        <xsl:text disable-output-escaping="yes">&lt;!--</xsl:text>
            <math>
                <xsl:apply-templates select="@* | node()"/>   
            </math>--<xsl:text disable-output-escaping="yes">&gt;</xsl:text> 
    </xsl:template>
    <xsl:template match="script">
        <xsl:text disable-output-escaping="yes">&lt;!--</xsl:text>
        <script>
            <xsl:apply-templates select="@* | node()"/>   
        </script>--<xsl:text disable-output-escaping="yes">&gt;</xsl:text> 
    </xsl:template>  
    <xsl:template match="li[@class='done']" priority =".7"> <!-- greater than .5 but below so def won't clash with bib stuff etc-->
        <li><xsl:text>✔ </xsl:text>
            <xsl:apply-templates select="@*[name()!='class'] | node()"/> 
        </li>
    </xsl:template>
    <xsl:template match="li[@class='notdone']" priority =".7">
        <li><xsl:text>✗ </xsl:text>
            <xsl:apply-templates select="@*[name()!='class'] | node()"/> 
        </li>
    </xsl:template>
    <xsl:template match="a">
        <link>
            <xsl:apply-templates select="@href | @title | node()"/> 
        </link>
    </xsl:template>
    <xsl:template match="img">
        <image>
            <xsl:attribute name="src"><xsl:value-of select="concat('..',replace(./@src,'static','webcontent'))"/></xsl:attribute>
            <xsl:if test="./@height"> <!--if's avoid making the attribute NaN if it never existed in the first place. Only encountered in problems so far but putting here in case-->
                <xsl:attribute name="height"><xsl:value-of select="round(number(./@height))"/></xsl:attribute> <!--ran into in kth ramp i that these must be positive integers and sometimes weren't-->
            </xsl:if>
            <xsl:if test="./@width">
                <xsl:attribute name="width"><xsl:value-of select="round(number(./@width))"/></xsl:attribute>
            </xsl:if>
            <xsl:apply-templates select="@alt | @title | node()"/> 
        </image>
    </xsl:template>

<!--Some further quickpass things - TO HANDLE BETTER LATER (these come out of attempting to mass process the entire content presentation chapter pages content with existing xslt)-->
    <!--sections with no headers. Curently made into not sections.-->
    <xsl:template match="section[not(h1) and not(h2) and not(h3) and not(h4) and not(h5) and not(h6)]">
        <xsl:apply-templates select="@* | node()"/> 
    </xsl:template>
    <!--badly nested lists--> <!--priority added so it takes priority over just processing the item (specifically at the moment the template that strips attributes)-->
    <xsl:template match="ul/ul | ol/ul" priority = "1">
        <li>
            <ul>
                <xsl:apply-templates/> <!--later will need to change to  select="@* | node()" and handle attributes-->
            </ul>
        </li>
    </xsl:template>
    <xsl:template match="ul/ol | ol/ol" priority = "1">
        <li>
            <ol>
                <xsl:apply-templates/> <!--later will need to change to  select="@* | node()" and handle attributes-->
            </ol>
        </li>
    </xsl:template>
    <!--currently just being erased as tags. Will need to be improved: divs and spans might have functional attributes (some divs seem to have hidden), and br's are functional-->
    <xsl:template match="div|span|br|center">
        <xsl:apply-templates/> 
    </xsl:template>
    <xsl:template match="p/br|p/span/br"> <!--IMPORTANT: span part will def need to be changed if I change how spans are handled in some ways-->
        <xsl:text disable-output-escaping="yes">&lt;/p&gt;&lt;p&gt;</xsl:text> <!--update: for brs in p's, attempting to split the paragraph at any brs-->
    </xsl:template>
    <!--list tags having not-permitted attributes stripped out (want to go through and handle better later, probably when have all the pages)-->
    <xsl:template match="li[@*]">
        <li><xsl:apply-templates select="@title | node()"/></li>
    </xsl:template>
    <xsl:template match="ul[@*]">
        <ul><xsl:apply-templates select="@title | node()"/></ul>
    </xsl:template>
    <xsl:template match="ol[@*]">
        <ol><xsl:apply-templates select="@title | node()"/></ol>
    </xsl:template>
    <!--currently being erased-->
    <xsl:template match="@align"></xsl:template>
    <xsl:template match="@class"></xsl:template>
    <!--used to comment this out, but kth ramp i has some text in them, so now stripping-->
    <xsl:template match="g">
        <xsl:apply-templates/>
    </xsl:template>
    <!--list tags having not-permitted attributes stripped out (want to go through and handle better later, probably when have all the pages)-->
        <!--additional note, unconventional_examples.xml, which has iframes, also has download links. Those go through fine but we may want to do something about them since they're not our servers.-->
    <xsl:template match="iframe">
        <iframe><xsl:apply-templates select="@src | @height | @width | node()"/></iframe>
    </xsl:template>
    <!--table pieces currently having their attributes all stripped out-->
    <xsl:template match="table"><table><xsl:apply-templates/></table></xsl:template>
    <xsl:template match="tr"><tr><xsl:apply-templates/></tr></xsl:template>
    <xsl:template match="th"><th><xsl:apply-templates/></th></xsl:template>
    <xsl:template match="td"><td><xsl:apply-templates/></td></xsl:template>
    <!--we don't have tbody-->
    <xsl:template match="tbody"><xsl:apply-templates/></xsl:template>
    
    <!--having an issue with bare text inside section bodies that used to be in divs-->
    <xsl:template match="div/text()[matches(.,'\S')]">
        <p>
            <xsl:value-of select="."/>
        </p>
    </xsl:template>
    
    <!--blockquotes don't allow paragraphs. At the moment making it a notation pullout; will consider more later-->
    <xsl:template match="blockquote[p]">
        <pullout type="notation"><xsl:apply-templates select="@* | node()"/></pullout>
    </xsl:template>
    <!--had an issue like the bare text one, except it used to be in a span in a div, with a bold-->
        <!--bold and italics-->
    <xsl:template match="div/strong | div/b | div/span/strong | div/span/b">
        <p><em style="bold"><xsl:apply-templates select="@* | node()"/></em></p>
    </xsl:template>
    <xsl:template match="div/i | div/em | div/span/i | div/span/em"> <!--POSSIBLY CHECK note, we also allow a plain em tag but it looks like it produces bold italics and html em is usually italics?-->
        <p><em style="italic"><xsl:apply-templates select="@* | node()"/></em></p>
    </xsl:template>
    <!--basically table titles-->
    <xsl:template match="div[h2 and div/table]">
        <table>
            <title><xsl:value-of select="./h2"/></title>
            <xsl:apply-templates select="./descendant::table/*"/> <!--not doing the attributes since table attributes are currently being stripped out in general-->
        </table>
    </xsl:template>

<!--Some further quickpass things - TO HANDLE BETTER LATER (from trying to process chapter course structure)-->
    <!--we do not allow empty tr's-->
    <xsl:template match="tr[not(th) and not(td)]"/>
    
<!--Some further quickpass things - TO HANDLE BETTER LATER (from trying to process chapter introduction)-->
    <!--a bunch of stuff is inside an article tag. Need to figure out later if that's doing anything-->
    <xsl:template match="article">
            <xsl:apply-templates select="@* | node()"/>
    </xsl:template>
    <!--we don't have buttons I think-->
    <xsl:template match="button">
        <xsl:text disable-output-escaping="yes">&lt;!--</xsl:text>
        <button>
            <xsl:apply-templates select="@* | node()"/>   
        </button>--<xsl:text disable-output-escaping="yes">&gt;</xsl:text> 
    </xsl:template>
    
<!--Some further quickpass things - TO HANDLE BETTER LATER (from trying to philanthropy course)-->
    <!--having a problem with ending up with 'bare' activity links inside body. (I checked and I think it'd also be a problem in a section body, so I can have 'in section' as a condition (since that would get turned into 'in section body' in the output), which is good bc it ends up in body from being in a headerless section so body/ wouldn't catch it))-->
    <!--this is literally the code that I have for making activity links copied over. There's probably a better way to do this that is less redundant-->
    <xsl:template match="body/a[matches(@href,'/jump_to_id/[a-z0-9]+')] | section/a[matches(@href,'/jump_to_id/[a-z0-9]+')]" priority="1">
        <xsl:variable name="pagetarget" select="tokenize(./@href,'/')[last()]"/>
        <xsl:variable name="olipageid" select="document($pagestable)/pages/page[@filename=$pagetarget]/id/@pageid"/> <!--in a variable since I'll use it twice-->
        <p>
            <activity_link>
                <xsl:attribute name="idref">
                    <xsl:value-of select="$olipageid"/>
                </xsl:attribute>
                <xsl:attribute name="title"> <!--for accessibility-->
                    <xsl:value-of select="$olipageid"/>
                </xsl:attribute>
                <xsl:apply-templates select="@*[name()!='href' and name()!='title'] | node()"/> <!--the internal links I saw didn't have titles but in case some do-->
            </activity_link>
        </p>
    </xsl:template>
   
<!--from KTH ramp i-->
    <!--having an issue with images ending up in bold tags which is not allowed. This is def overspecific to the problem but don't have a general solution atm.-->
    <xsl:template match="b[span[img]]">
        <!--was originally trying to retain the bolding on any other children, but it's complicated and then I found one of these *spans* having other children, and I think really I'll just strip the bold atm.-->
        <xsl:comment>was bold, start</xsl:comment><xsl:apply-templates/><xsl:comment>was bold, end</xsl:comment>
            <!--I want to strip out the bold tags around the span with the image, but if there was anything else in the b I want to keep the bold tags on it-->
        <!--<xsl:for-each select="*">
            <xsl:choose>
                <xsl:when test="./span[img]">
                    <xsl:apply-templates select="."/>
                </xsl:when>
                <xsl:otherwise>
                    <em style="bold"><xsl:apply-templates select="."/></em>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>  -->     
    </xsl:template>
    
    <xsl:template match="iframe/text()"> <!--have some text in an iframe, which is not allowed. Picked a tag from ones that are said to be allowed in iframes-->
        <caption>
            <xsl:value-of select="."/>
        </caption>
    </xsl:template>
</xsl:stylesheet>