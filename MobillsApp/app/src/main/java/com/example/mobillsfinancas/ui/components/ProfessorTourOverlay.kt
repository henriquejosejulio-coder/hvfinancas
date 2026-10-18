package com.example.mobillsfinancas.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobillsfinancas.theme.MobillsGreenPrimary

data class ProfessorStepInfo(
    val step: Int,
    val targetTab: Int,
    val title: String,
    val description: String,
    val tip: String? = null,
    val hasFormAction: Boolean = false,
    val actionButtonText: String = "Próximo ▶"
)

val professorSteps = listOf(
    ProfessorStepInfo(
        step = 1,
        targetTab = 0,
        title = "Bem-vindo ao HV Finanças! 🎓",
        description = "Olá! Eu sou o seu Professor no HV Finanças. Vou te guiar por cada tela e ferramenta do aplicativo para você ter controle total do seu dinheiro de forma fácil, prática e sem complicação!",
        tip = "No topo você acompanha seu Saldo Geral consolidado. Toque no ícone do olhinho (👁️/🙈) para ocultar seus valores em público com segurança!",
        actionButtonText = "Começar Tour ▶"
    ),
    ProfessorStepInfo(
        step = 2,
        targetTab = 0,
        title = "Navegação de Mês & Balanço Geral",
        description = "Use as setas ◀ e ▶ para navegar no tempo (conferir meses anteriores ou se planejar para o futuro). Os 3 cartões principais resumem seu mês:\n• Verde: Total de Receitas que entraram\n• Vermelho: Total de Despesas do mês\n• Azul: O que Sobrou no seu bolso até o fim do mês.",
        tip = "Você pode fazer lançamentos em meses futuros para se antecipar a contas e salários!",
        actionButtonText = "Próximo ▶"
    ),
    ProfessorStepInfo(
        step = 3,
        targetTab = 0,
        title = "Contas Correntes, Bancos & Carteiras",
        description = "Aqui ficam listadas suas contas (Inter, Mercado Pago, Nubank, Dinheiro em Carteira, etc.). Você pode adicionar novas contas no (+), fazer transferências entre elas e tocar em 'Ajustar' para alinhar seu saldo centavo por centavo com seu extrato real.",
        tip = "Quando você transfere dinheiro entre suas contas, o app atualiza os dois saldos automaticamente!",
        actionButtonText = "Próximo ▶"
    ),
    ProfessorStepInfo(
        step = 4,
        targetTab = 0,
        title = "Cartões de Crédito & Limites",
        description = "Visualize rapidamente seus cartões de crédito cadastrados, o limite disponível para novas compras e o valor da fatura atual em aberto. Deslize para o lado para ver todos os seus cartões.",
        tip = "Aqui no início você tem a visão resumida; na aba 'Cartões' você pode ver as faturas e parcelas futuras em detalhes!",
        actionButtonText = "Próximo ▶"
    ),
    ProfessorStepInfo(
        step = 5,
        targetTab = 0,
        title = "Leitor Automático de Bancos ⚡",
        description = "O grande diferencial do aplicativo! Quando você faz uma compra no cartão ou recebe/envia um Pix, o HV Finanças lê a notificação do seu banco e cadastra a transação na hora, sem você precisar digitar nada!",
        tip = "Notificações de marketing ou rendimento são filtradas automaticamente. Se a compra for parcelada, aparecem botões rápidos (2x, 3x, 10x) para você ajustar em 1 toque!",
        actionButtonText = "Próximo ▶"
    ),
    ProfessorStepInfo(
        step = 6,
        targetTab = 0,
        title = "Botão de Inclusão (+) & Formulário",
        description = "O botão verde flutuante (+) permite lançar Despesas, Receitas ou Transferências manualmente. Nele você escolhe categorias, contas/cartões, compras parceladas em até 12x ou contas fixas todo mês.",
        tip = "Toque em 'Ver Formulário (+)' para abrir o formulário agora com seu tutorial exclusivo de 6 passos, ou continue para conhecer as outras telas!",
        hasFormAction = true,
        actionButtonText = "Próxima Tela ▶"
    ),
    ProfessorStepInfo(
        step = 7,
        targetTab = 1,
        title = "Aba Extrato: Histórico Completo",
        description = "Aqui está o extrato detalhado de tudo o que aconteceu no mês. Você pode buscar lançamentos por texto, filtrar por status (Pago ou Pendente) e selecionar múltiplos itens para ações em lote.",
        tip = "Toque no círculo ao lado de qualquer lançamento para alternar rapidamente entre Pago (verde) e Pendente (cinza)!",
        actionButtonText = "Próximo ▶"
    ),
    ProfessorStepInfo(
        step = 8,
        targetTab = 2,
        title = "Aba Cartões: Faturas & Parcelas Futuras",
        description = "Gerencie seus cartões de crédito a fundo! Acompanhe a fatura atual e veja o provisionamento de parcelas dos próximos meses para nunca ser pego de surpresa.",
        tip = "Ao pagar a fatura, use o botão 'Pagar Fatura': escolha qual conta bancária pagou e o app debita o saldo e zera a fatura com 1 clique!",
        actionButtonText = "Próximo ▶"
    ),
    ProfessorStepInfo(
        step = 9,
        targetTab = 3,
        title = "Aba Tags & Categorias",
        description = "Deixe o app com a sua cara! Crie e edite categorias de despesas e receitas com cores e emojis. Crie também Tags (ex: #viagem, #carro, #faculdade) para agrupar despesas de categorias diferentes.",
        tip = "Você pode definir metas e limites de orçamento por categoria para não estourar seus gastos!",
        actionButtonText = "Próximo ▶"
    ),
    ProfessorStepInfo(
        step = 10,
        targetTab = 4,
        title = "Aba Bancos: Sincronização & Backup",
        description = "Configure a leitura automática de notificações dos bancos, importe extratos OFX ou CSV, e faça Backup completo dos seus dados para salvar no Google Drive e nunca perder suas informações.",
        tip = "Faça backups regulares! Se você formatar o celular ou trocar de aparelho, basta restaurar o arquivo JSON para ter tudo de volta intacto.",
        actionButtonText = "Próximo ▶"
    ),
    ProfessorStepInfo(
        step = 11,
        targetTab = 0,
        title = "Parabéns! Você concluiu o Tour! 🎓🚀",
        description = "Agora você conhece todas as ferramentas do HV Finanças e está 100% pronto para organizar sua vida financeira com praticidade e inteligência!\n\nSempre que quiser rever este tutorial, basta tocar no ícone do capelo 🎓 no topo da tela ou na aba Bancos.",
        tip = "O controle do seu dinheiro agora está nas suas mãos. Sucesso nos seus objetivos financeiros!",
        actionButtonText = "Concluir e Começar a Usar 🚀"
    )
)

@Composable
fun ProfessorTourOverlay(
    currentStep: Int,
    totalSteps: Int = professorSteps.size,
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit,
    onOpenForm: () -> Unit,
    onSkipTour: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stepInfo = professorSteps.find { it.step == currentStep } ?: professorSteps.first()

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
            border = BorderStroke(2.dp, MobillsGreenPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Cabeçalho: Avatar + Título + Botão Fechar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MobillsGreenPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👨‍🏫", fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Modo Professor",
                                    fontWeight = FontWeight.Bold,
                                    color = MobillsGreenPrimary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MobillsGreenPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "$currentStep de $totalSteps",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = MobillsGreenPrimary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = stepInfo.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = onSkipTour,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Barra de Progresso
                LinearProgressIndicator(
                    progress = { currentStep.toFloat() / totalSteps.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MobillsGreenPrimary,
                    trackColor = MobillsGreenPrimary.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Descrição Educativa
                Text(
                    text = stepInfo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )

                // Dica Especial (se houver)
                if (!stepInfo.tip.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("💡", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stepInfo.tip,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Linha de Ações: Pular / Voltar / Próximo / Ação Especial
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = onPrevStep,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("◀ Voltar", fontSize = 12.sp)
                        }
                    } else {
                        TextButton(
                            onClick = onSkipTour,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Pular Tutorial",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (stepInfo.hasFormAction) {
                            OutlinedButton(
                                onClick = onOpenForm,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MobillsGreenPrimary
                                ),
                                border = BorderStroke(1.5.dp, MobillsGreenPrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Ver Formulário (+)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onNextStep,
                            colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stepInfo.actionButtonText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
